//! AIGC vote backend service.

use std::collections::{HashMap, HashSet};
use std::net::SocketAddr;
use std::sync::atomic::{AtomicBool, AtomicI64, Ordering};
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use async_trait::async_trait;
use axum::extract::{Path as AxumPath, State};
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::routing::{get, post};
use axum::{Json, Router};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use sqlx::postgres::PgPoolOptions;
use sqlx::{PgPool, Postgres, Row, Transaction};

const CREDIT_CAP: i64 = 5;
const INITIAL_CREDIT: i64 = 1;
const READS_PER_CREDIT: i64 = 100;
const MIN_READ_DURATION_MS: i64 = 15_000;
const MIN_READ_SCROLL_RATIO: f64 = 0.25;
const MIN_FLAG_DURATION_MS: i64 = 15_000;
const MIN_FLAG_SCROLL_DEPTH: f64 = 0.25;
const EXTERNAL_AIGC_SOURCE: &str = "zhihuai";
const EXTERNAL_AIGC_BASE_URL: &str = "https://zhihuai.sx349.xyz";
const EXTERNAL_AIGC_TIMEOUT_SECONDS: u64 = 5;
const EXTERNAL_AIGC_LEADERBOARD_REFRESH_INTERVAL_SECONDS: i64 = 300;
static EXTERNAL_AIGC_LEADERBOARD_REFRESH_IN_FLIGHT: AtomicBool = AtomicBool::new(false);
static EXTERNAL_AIGC_LEADERBOARD_LAST_REFRESH_ATTEMPT_AT: AtomicI64 = AtomicI64::new(0);

#[derive(Clone)]
pub struct AppState {
    store: Arc<dyn VoteStore>,
}

impl AppState {
    pub fn new_in_memory() -> Result<Self, ServiceError> {
        Self::new_in_memory_with_credit_bypass_voters(HashSet::new())
    }

    pub fn new_in_memory_with_credit_bypass_voters(
        credit_bypass_voters: HashSet<String>,
    ) -> Result<Self, ServiceError> {
        Ok(Self {
            store: Arc::new(MemoryVoteStore::new(credit_bypass_voters)),
        })
    }

    pub async fn new_postgres(database_url: &str) -> Result<Self, ServiceError> {
        Self::new_postgres_with_credit_bypass_voters(database_url, HashSet::new()).await
    }

    pub async fn new_postgres_with_credit_bypass_voters(
        database_url: &str,
        credit_bypass_voters: HashSet<String>,
    ) -> Result<Self, ServiceError> {
        Ok(Self {
            store: Arc::new(PostgresVoteStore::connect(database_url, credit_bypass_voters).await?),
        })
    }
}

pub fn parse_credit_bypass_voters(value: &str) -> HashSet<String> {
    value
        .split(',')
        .filter_map(|item| normalize_identity_token(item))
        .collect()
}

#[derive(Debug, Deserialize, Serialize)]
pub struct ReadEventsRequest {
    pub client_id: String,
    pub events: Vec<ReadEvent>,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct ReadEvent {
    pub content_type: String,
    pub content_id: String,
    #[serde(default)]
    pub title: String,
    #[serde(default)]
    pub author_hash: String,
    pub content_html: String,
    pub content_updated_at: i64,
    pub opened_at: i64,
    pub foreground_duration_ms: i64,
    pub max_scroll_ratio: f64,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct ReadEventsResponse {
    pub credit: i64,
    pub progress: i64,
    pub cap: i64,
    pub accepted_events: i64,
    pub rejected_events: i64,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct AigcFlagRequest {
    pub client_id: String,
    pub voter: VoterIdentity,
    pub title: String,
    pub author_hash: String,
    pub content_html: String,
    pub content_updated_at: i64,
    pub evidence: AigcFlagEvidence,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct VoterIdentity {
    pub id: String,
    pub name: String,
    pub url_token: Option<String>,
    pub avatar_url: Option<String>,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct AigcFlagEvidence {
    pub client_view_duration_ms: i64,
    pub scroll_depth: f64,
    pub opened_at: i64,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct AigcFlagResponse {
    pub my_flagged: bool,
    pub credit: i64,
    pub credit_bypass_available: bool,
    /// 自家后端支持人数：每个有效的 Zhihu++ AIGC 标记用户计 1 人。
    pub effective_flag_count: i64,
    /// 自家后端原始支持人数；当前与 effective_flag_count 相同。
    pub raw_flag_count: i64,
    /// 自家后端中当前正文 HTML 版本的支持人数。
    pub current_version_flag_count: i64,
    pub content_hash: String,
    pub content_updated_at: i64,
    pub confidence: String,
    pub voters: Vec<AigcFlagVoter>,
    /// 来自 zhihuai.sx349.xyz 的外部数据源统计。
    /// 客户端展示总人数时，应使用 effective_flag_count + external_source.voter_count。
    pub external_source: Option<ExternalAigcSource>,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct AigcFlagStatusResponse {
    pub my_flagged: bool,
    pub credit: i64,
    pub progress: i64,
    pub cap: i64,
    pub credit_bypass_available: bool,
    /// 自家后端支持人数：每个有效的 Zhihu++ AIGC 标记用户计 1 人。
    pub effective_flag_count: i64,
    /// 自家后端原始支持人数；当前与 effective_flag_count 相同。
    pub raw_flag_count: i64,
    pub confidence: String,
    pub voters: Vec<AigcFlagVoter>,
    /// 来自 zhihuai.sx349.xyz 的外部数据源统计。
    /// 客户端展示总人数时，应使用 effective_flag_count + external_source.voter_count。
    pub external_source: Option<ExternalAigcSource>,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct AigcFlagVoter {
    pub voter_id: String,
    pub voter_name: String,
    pub voter_url_token: Option<String>,
    pub voter_avatar_url: Option<String>,
    pub created_at: i64,
    pub credit_bypassed: bool,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct ExternalAigcSource {
    pub source: String,
    pub content_type: String,
    pub content_id: String,
    /// zhihuai 支持票数，含义是“疑似AI生成低质量内容”。
    pub total_votes: i64,
    /// zhihuai 支持票投票人数；这是外部来源的 AIGC 支持人数。
    pub voter_count: i64,
    /// zhihuai 反对票数，含义是“并非AI生成低质量内容”。
    pub total_downvotes: i64,
    /// zhihuai 反对票投票人数；这不计入 AIGC 支持人数。
    pub downvoter_count: i64,
    pub refreshed_at: i64,
}

#[derive(Debug, Deserialize)]
pub struct AigcFlagStatusQuery {
    pub client_id: Option<String>,
    pub voter_id: Option<String>,
    pub voter_name: Option<String>,
    pub voter_url_token: Option<String>,
}

#[derive(Debug)]
pub struct ServiceError {
    status: StatusCode,
    message: String,
}

impl ServiceError {
    fn bad_request(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::BAD_REQUEST,
            message: message.into(),
        }
    }

    fn payment_required(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::PAYMENT_REQUIRED,
            message: message.into(),
        }
    }

    fn internal(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::INTERNAL_SERVER_ERROR,
            message: message.into(),
        }
    }
}

impl From<sqlx::Error> for ServiceError {
    fn from(value: sqlx::Error) -> Self {
        Self::internal(value.to_string())
    }
}

impl IntoResponse for ServiceError {
    fn into_response(self) -> Response {
        (
            self.status,
            Json(serde_json::json!({
                "error": self.message,
            })),
        )
            .into_response()
    }
}

pub fn app(state: AppState) -> Router {
    Router::new()
        .route("/healthz", get(healthz))
        .route("/v1/read-events:batch", post(post_read_events))
        .route(
            "/v1/contents/{content_type}/{content_id}/aigc-flag",
            get(get_flag_status).post(post_aigc_flag),
        )
        .with_state(state)
}

pub async fn serve(state: AppState, addr: SocketAddr) -> Result<(), ServiceError> {
    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))?;
    axum::serve(listener, app(state))
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))
}

async fn healthz(State(state): State<AppState>) -> Result<Json<serde_json::Value>, ServiceError> {
    state.store.health().await?;
    Ok(Json(serde_json::json!({ "ok": true })))
}

async fn post_read_events(
    State(state): State<AppState>,
    Json(request): Json<ReadEventsRequest>,
) -> Result<Json<ReadEventsResponse>, ServiceError> {
    validate_client_id(&request.client_id)?;
    Ok(Json(state.store.record_read_events(request).await?))
}

async fn post_aigc_flag(
    State(state): State<AppState>,
    AxumPath((content_type, content_id)): AxumPath<(String, String)>,
    Json(request): Json<AigcFlagRequest>,
) -> Result<Json<AigcFlagResponse>, ServiceError> {
    validate_client_id(&request.client_id)?;
    validate_content_identity(&content_type, &content_id)?;
    validate_flag_request(&request)?;

    Ok(Json(
        state
            .store
            .submit_aigc_flag(content_type, content_id, request)
            .await?,
    ))
}

async fn get_flag_status(
    State(state): State<AppState>,
    AxumPath((content_type, content_id)): AxumPath<(String, String)>,
    axum::extract::Query(query): axum::extract::Query<AigcFlagStatusQuery>,
) -> Result<Json<AigcFlagStatusResponse>, ServiceError> {
    validate_content_identity(&content_type, &content_id)?;

    Ok(Json(
        state
            .store
            .flag_status(
                content_type,
                content_id,
                query.client_id,
                query.voter_id,
                query.voter_name,
                query.voter_url_token,
            )
            .await?,
    ))
}

#[async_trait]
trait VoteStore: Send + Sync {
    async fn health(&self) -> Result<(), ServiceError>;

    async fn record_read_events(
        &self,
        request: ReadEventsRequest,
    ) -> Result<ReadEventsResponse, ServiceError>;

    async fn submit_aigc_flag(
        &self,
        content_type: String,
        content_id: String,
        request: AigcFlagRequest,
    ) -> Result<AigcFlagResponse, ServiceError>;

    async fn flag_status(
        &self,
        content_type: String,
        content_id: String,
        client_id: Option<String>,
        voter_id: Option<String>,
        voter_name: Option<String>,
        voter_url_token: Option<String>,
    ) -> Result<AigcFlagStatusResponse, ServiceError>;
}

struct MemoryVoteStore {
    data: Mutex<MemoryData>,
    credit_bypass_voters: HashSet<String>,
}

impl MemoryVoteStore {
    fn new(credit_bypass_voters: HashSet<String>) -> Self {
        Self {
            data: Mutex::new(MemoryData::default()),
            credit_bypass_voters: normalize_identity_tokens(credit_bypass_voters),
        }
    }
}

#[derive(Default)]
struct MemoryData {
    clients: HashMap<String, ClientRecord>,
    read_events: HashSet<ReadEventKey>,
    snapshots: HashSet<SnapshotKey>,
    flags: HashMap<FlagKey, MemoryFlagRecord>,
}

#[derive(Debug)]
struct ClientRecord {
    account: ClientAccount,
    last_seen_at: i64,
}

#[derive(Debug, Hash, Eq, PartialEq)]
struct ReadEventKey {
    client_id: String,
    content_type: String,
    content_id: String,
}

#[derive(Debug, Hash, Eq, PartialEq)]
struct SnapshotKey {
    content_type: String,
    content_id: String,
    content_hash: String,
}

#[derive(Debug, Clone, Hash, Eq, PartialEq)]
struct FlagKey {
    voter_id: String,
    content_type: String,
    content_id: String,
}

#[derive(Debug)]
struct MemoryFlagRecord {
    content_hash: String,
    voter: VoterIdentity,
    created_at: i64,
    credit_bypassed: bool,
}

impl MemoryData {
    fn ensure_client(&mut self, client_id: &str) {
        let now = now_epoch_seconds();
        self.clients
            .entry(client_id.to_string())
            .and_modify(|client| {
                client.last_seen_at = now;
            })
            .or_insert_with(|| ClientRecord {
                account: ClientAccount {
                    credit: INITIAL_CREDIT,
                    progress: 0,
                },
                last_seen_at: now,
            });
    }

    fn client_account(&self, client_id: &str) -> Result<ClientAccount, ServiceError> {
        self.clients
            .get(client_id)
            .map(|client| client.account)
            .ok_or_else(|| ServiceError::internal("client account missing"))
    }

    fn flag_count(&self, content_type: &str, content_id: &str) -> i64 {
        self.flags
            .keys()
            .filter(|key| key.content_type == content_type && key.content_id == content_id)
            .count() as i64
    }

    fn current_version_flag_count(
        &self,
        content_type: &str,
        content_id: &str,
        content_hash: &str,
    ) -> i64 {
        self.flags
            .iter()
            .filter(|(key, record)| {
                key.content_type == content_type
                    && key.content_id == content_id
                    && record.content_hash == content_hash
            })
            .count() as i64
    }

    fn flag_voters(&self, content_type: &str, content_id: &str) -> Vec<AigcFlagVoter> {
        let mut voters = self
            .flags
            .iter()
            .filter(|(key, _)| key.content_type == content_type && key.content_id == content_id)
            .map(|(_, record)| {
                flag_voter_from_identity(&record.voter, record.created_at, record.credit_bypassed)
            })
            .collect::<Vec<_>>();
        voters.sort_by(|left, right| right.created_at.cmp(&left.created_at));
        voters.truncate(10);
        voters
    }
}

#[async_trait]
impl VoteStore for MemoryVoteStore {
    async fn health(&self) -> Result<(), ServiceError> {
        Ok(())
    }

    async fn record_read_events(
        &self,
        request: ReadEventsRequest,
    ) -> Result<ReadEventsResponse, ServiceError> {
        let mut data = self
            .data
            .lock()
            .map_err(|_| ServiceError::internal("memory store lock poisoned"))?;
        data.ensure_client(&request.client_id);

        let mut accepted_events = 0_i64;
        let mut rejected_events = 0_i64;
        for event in request.events {
            if !is_valid_read_event(&event) {
                rejected_events += 1;
                continue;
            }

            let key = ReadEventKey {
                client_id: request.client_id.clone(),
                content_type: event.content_type,
                content_id: event.content_id,
            };
            if data.read_events.insert(key) {
                accepted_events += 1;
            }
        }

        if accepted_events > 0 {
            let client = data
                .clients
                .get_mut(&request.client_id)
                .ok_or_else(|| ServiceError::internal("client account missing"))?;
            apply_credit_progress_to_account(&mut client.account, accepted_events);
            client.last_seen_at = now_epoch_seconds();
        }

        let account = data.client_account(&request.client_id)?;
        Ok(ReadEventsResponse {
            credit: account.credit,
            progress: account.progress,
            cap: CREDIT_CAP,
            accepted_events,
            rejected_events,
        })
    }

    async fn submit_aigc_flag(
        &self,
        content_type: String,
        content_id: String,
        request: AigcFlagRequest,
    ) -> Result<AigcFlagResponse, ServiceError> {
        let content_hash = sha256_hex(&request.content_html);
        let credit_bypass_available = is_credit_bypass_voter(
            &self.credit_bypass_voters,
            &request.client_id,
            &request.voter,
        );
        let mut data = self
            .data
            .lock()
            .map_err(|_| ServiceError::internal("memory store lock poisoned"))?;
        data.ensure_client(&request.client_id);

        data.snapshots.insert(SnapshotKey {
            content_type: content_type.clone(),
            content_id: content_id.clone(),
            content_hash: content_hash.clone(),
        });

        let flag_key = FlagKey {
            voter_id: request.voter.id.clone(),
            content_type: content_type.clone(),
            content_id: content_id.clone(),
        };
        if !data.flags.contains_key(&flag_key) {
            let client = data
                .clients
                .get_mut(&request.client_id)
                .ok_or_else(|| ServiceError::internal("client account missing"))?;
            if !credit_bypass_available {
                validate_flag_evidence(&request)?;
            }
            if client.account.credit <= 0 && !credit_bypass_available {
                return Err(ServiceError::payment_required(
                    "not enough AIGC vote credit",
                ));
            }
            if !credit_bypass_available {
                client.account.credit -= 1;
            }
            client.last_seen_at = now_epoch_seconds();
            data.flags.insert(
                flag_key,
                MemoryFlagRecord {
                    content_hash: content_hash.clone(),
                    voter: request.voter.clone(),
                    created_at: now_epoch_seconds(),
                    credit_bypassed: credit_bypass_available,
                },
            );
        }

        let account = data.client_account(&request.client_id)?;
        let count = data.flag_count(&content_type, &content_id);
        let current_version_count =
            data.current_version_flag_count(&content_type, &content_id, &content_hash);
        let voters = data.flag_voters(&content_type, &content_id);

        Ok(AigcFlagResponse {
            my_flagged: true,
            credit: account.credit,
            credit_bypass_available,
            effective_flag_count: count,
            raw_flag_count: count,
            current_version_flag_count: current_version_count,
            content_hash,
            content_updated_at: request.content_updated_at,
            confidence: confidence_for_count(count),
            voters,
            external_source: None,
        })
    }

    async fn flag_status(
        &self,
        content_type: String,
        content_id: String,
        client_id: Option<String>,
        voter_id: Option<String>,
        voter_name: Option<String>,
        voter_url_token: Option<String>,
    ) -> Result<AigcFlagStatusResponse, ServiceError> {
        let mut data = self
            .data
            .lock()
            .map_err(|_| ServiceError::internal("memory store lock poisoned"))?;
        let client_id = client_id.unwrap_or_default();
        let voter_id = voter_id.unwrap_or_default();
        let voter_name = voter_name.unwrap_or_default();
        let voter_url_token = voter_url_token.unwrap_or_default();
        let credit_bypass_available = is_credit_bypass_status_query(
            &self.credit_bypass_voters,
            &client_id,
            &voter_id,
            &voter_name,
            &voter_url_token,
        );
        let (credit, progress, my_flagged) = if client_id.is_blank() {
            (0, 0, false)
        } else {
            data.ensure_client(&client_id);
            let account = data.client_account(&client_id)?;
            let flagged = data.flags.contains_key(&FlagKey {
                voter_id: if voter_id.is_blank() {
                    client_id.clone()
                } else {
                    voter_id.clone()
                },
                content_type: content_type.clone(),
                content_id: content_id.clone(),
            });
            (account.credit, account.progress, flagged)
        };
        let count = data.flag_count(&content_type, &content_id);
        let voters = data.flag_voters(&content_type, &content_id);

        Ok(AigcFlagStatusResponse {
            my_flagged,
            credit,
            progress,
            cap: CREDIT_CAP,
            credit_bypass_available,
            effective_flag_count: count,
            raw_flag_count: count,
            confidence: confidence_for_count(count),
            voters,
            external_source: None,
        })
    }
}

struct PostgresVoteStore {
    pool: PgPool,
    credit_bypass_voters: HashSet<String>,
}

impl PostgresVoteStore {
    async fn connect(
        database_url: &str,
        credit_bypass_voters: HashSet<String>,
    ) -> Result<Self, ServiceError> {
        let pool = PgPoolOptions::new()
            .max_connections(10)
            .connect(database_url)
            .await?;
        initialize_postgres_schema(&pool).await?;
        Ok(Self {
            pool,
            credit_bypass_voters: normalize_identity_tokens(credit_bypass_voters),
        })
    }
}

#[async_trait]
impl VoteStore for PostgresVoteStore {
    async fn health(&self) -> Result<(), ServiceError> {
        sqlx::query_scalar::<_, i64>("SELECT 1::BIGINT")
            .fetch_one(&self.pool)
            .await?;
        Ok(())
    }

    async fn record_read_events(
        &self,
        request: ReadEventsRequest,
    ) -> Result<ReadEventsResponse, ServiceError> {
        let mut tx = self.pool.begin().await?;
        ensure_client_tx(&mut tx, &request.client_id).await?;

        let mut accepted_events = 0_i64;
        let mut rejected_events = 0_i64;
        for event in request.events {
            if !is_valid_read_event(&event) {
                rejected_events += 1;
                continue;
            }
            let content_hash = sha256_hex(&event.content_html);

            upsert_content_snapshot_fields_tx(
                &mut tx,
                &event.content_type,
                &event.content_id,
                &content_hash,
                event.content_updated_at,
                &event.title,
                &event.author_hash,
                &event.content_html,
            )
            .await?;

            let result = sqlx::query(
                r#"
                INSERT INTO read_events (
                    client_id,
                    content_type,
                    content_id,
                    content_hash,
                    content_updated_at,
                    opened_at,
                    foreground_duration_ms,
                    max_scroll_ratio
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                ON CONFLICT(client_id, content_type, content_id) DO NOTHING
                "#,
            )
            .bind(&request.client_id)
            .bind(&event.content_type)
            .bind(&event.content_id)
            .bind(&content_hash)
            .bind(event.content_updated_at)
            .bind(event.opened_at)
            .bind(event.foreground_duration_ms)
            .bind(event.max_scroll_ratio)
            .execute(&mut *tx)
            .await?;

            if result.rows_affected() == 1 {
                accepted_events += 1;
            }
        }

        apply_credit_progress_tx(&mut tx, &request.client_id, accepted_events).await?;
        let account = client_account_tx(&mut tx, &request.client_id, false).await?;
        tx.commit().await?;

        Ok(ReadEventsResponse {
            credit: account.credit,
            progress: account.progress,
            cap: CREDIT_CAP,
            accepted_events,
            rejected_events,
        })
    }

    async fn submit_aigc_flag(
        &self,
        content_type: String,
        content_id: String,
        request: AigcFlagRequest,
    ) -> Result<AigcFlagResponse, ServiceError> {
        let content_hash = sha256_hex(&request.content_html);
        let credit_bypass_available = is_credit_bypass_voter(
            &self.credit_bypass_voters,
            &request.client_id,
            &request.voter,
        );
        let mut tx = self.pool.begin().await?;
        ensure_client_tx(&mut tx, &request.client_id).await?;

        let already_flagged =
            has_existing_flag_tx(&mut tx, &request.voter.id, &content_type, &content_id).await?;
        upsert_content_snapshot_tx(&mut tx, &content_type, &content_id, &content_hash, &request)
            .await?;

        if !already_flagged {
            if !credit_bypass_available {
                validate_flag_evidence(&request)?;
            }
            let account = client_account_tx(&mut tx, &request.client_id, true).await?;
            if account.credit <= 0 && !credit_bypass_available {
                return Err(ServiceError::payment_required(
                    "not enough AIGC vote credit",
                ));
            }

            let result = sqlx::query(
                r#"
                INSERT INTO aigc_flags (
                    client_id,
                    content_type,
                    content_id,
                    content_hash,
                    content_updated_at,
                    voter_id,
                    voter_name,
                    voter_url_token,
                    voter_avatar_url,
                    credit_bypassed,
                    client_view_duration_ms,
                    scroll_depth,
                    opened_at,
                    created_at
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
                ON CONFLICT DO NOTHING
                "#,
            )
            .bind(&request.client_id)
            .bind(&content_type)
            .bind(&content_id)
            .bind(&content_hash)
            .bind(request.content_updated_at)
            .bind(&request.voter.id)
            .bind(&request.voter.name)
            .bind(&request.voter.url_token)
            .bind(&request.voter.avatar_url)
            .bind(credit_bypass_available)
            .bind(request.evidence.client_view_duration_ms)
            .bind(request.evidence.scroll_depth)
            .bind(request.evidence.opened_at)
            .bind(now_epoch_seconds())
            .execute(&mut *tx)
            .await?;

            if result.rows_affected() == 1 && !credit_bypass_available {
                sqlx::query(
                    "UPDATE clients SET credit = credit - 1, last_seen_at = $1 WHERE id = $2",
                )
                .bind(now_epoch_seconds())
                .bind(&request.client_id)
                .execute(&mut *tx)
                .await?;
            }
        }

        let mut response = build_flag_response_tx(
            &mut tx,
            &request.client_id,
            &content_type,
            &content_id,
            &content_hash,
            request.content_updated_at,
            credit_bypass_available,
        )
        .await?;
        tx.commit().await?;
        response.external_source =
            refresh_and_load_external_aigc_stats(&self.pool, &content_type, &content_id).await;

        Ok(response)
    }

    async fn flag_status(
        &self,
        content_type: String,
        content_id: String,
        client_id: Option<String>,
        voter_id: Option<String>,
        voter_name: Option<String>,
        voter_url_token: Option<String>,
    ) -> Result<AigcFlagStatusResponse, ServiceError> {
        let mut tx = self.pool.begin().await?;
        let client_id = client_id.unwrap_or_default();
        let voter_id = voter_id.unwrap_or_default();
        let voter_name = voter_name.unwrap_or_default();
        let voter_url_token = voter_url_token.unwrap_or_default();
        let credit_bypass_available = is_credit_bypass_status_query(
            &self.credit_bypass_voters,
            &client_id,
            &voter_id,
            &voter_name,
            &voter_url_token,
        );
        let (credit, progress, my_flagged) = if client_id.is_blank() {
            (0, 0, false)
        } else {
            ensure_client_tx(&mut tx, &client_id).await?;
            let account = client_account_tx(&mut tx, &client_id, false).await?;
            let checked_voter_id = if voter_id.is_blank() {
                &client_id
            } else {
                &voter_id
            };
            let flagged =
                has_existing_flag_tx(&mut tx, checked_voter_id, &content_type, &content_id).await?;
            (account.credit, account.progress, flagged)
        };
        let count = flag_count_tx(&mut tx, &content_type, &content_id).await?;
        let voters = flag_voters_tx(&mut tx, &content_type, &content_id).await?;
        tx.commit().await?;
        let external_source =
            refresh_and_load_external_aigc_stats(&self.pool, &content_type, &content_id).await;

        Ok(AigcFlagStatusResponse {
            my_flagged,
            credit,
            progress,
            cap: CREDIT_CAP,
            credit_bypass_available,
            effective_flag_count: count,
            raw_flag_count: count,
            confidence: confidence_for_count(count),
            voters,
            external_source,
        })
    }
}

async fn initialize_postgres_schema(pool: &PgPool) -> Result<(), ServiceError> {
    let statements = [
        r#"
        CREATE TABLE IF NOT EXISTS clients (
            id TEXT PRIMARY KEY NOT NULL,
            credit BIGINT NOT NULL DEFAULT 1,
            progress BIGINT NOT NULL DEFAULT 0,
            created_at BIGINT NOT NULL,
            last_seen_at BIGINT NOT NULL
        )
        "#,
        "ALTER TABLE clients ALTER COLUMN credit SET DEFAULT 1",
        r#"
        CREATE TABLE IF NOT EXISTS read_events (
            id BIGSERIAL PRIMARY KEY,
            client_id TEXT NOT NULL REFERENCES clients(id),
            content_type TEXT NOT NULL,
            content_id TEXT NOT NULL,
            content_hash TEXT NOT NULL,
            content_updated_at BIGINT NOT NULL,
            opened_at BIGINT NOT NULL,
            foreground_duration_ms BIGINT NOT NULL,
            max_scroll_ratio DOUBLE PRECISION NOT NULL,
            created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
            UNIQUE(client_id, content_type, content_id)
        )
        "#,
        "CREATE INDEX IF NOT EXISTS index_read_events_client_id ON read_events(client_id)",
        "CREATE INDEX IF NOT EXISTS index_read_events_content ON read_events(content_type, content_id)",
        r#"
        CREATE TABLE IF NOT EXISTS content_snapshots (
            content_type TEXT NOT NULL,
            content_id TEXT NOT NULL,
            content_hash TEXT NOT NULL,
            content_updated_at BIGINT NOT NULL,
            title TEXT NOT NULL,
            author_hash TEXT NOT NULL,
            content_html TEXT NOT NULL,
            first_seen_at BIGINT NOT NULL,
            PRIMARY KEY(content_type, content_id, content_hash)
        )
        "#,
        r#"
        CREATE TABLE IF NOT EXISTS aigc_flags (
            id BIGSERIAL PRIMARY KEY,
            client_id TEXT NOT NULL REFERENCES clients(id),
            content_type TEXT NOT NULL,
            content_id TEXT NOT NULL,
            content_hash TEXT NOT NULL,
            content_updated_at BIGINT NOT NULL,
            voter_id TEXT NOT NULL,
            voter_name TEXT NOT NULL,
            voter_url_token TEXT,
            voter_avatar_url TEXT,
            credit_bypassed BOOLEAN NOT NULL DEFAULT FALSE,
            client_view_duration_ms BIGINT NOT NULL,
            scroll_depth DOUBLE PRECISION NOT NULL,
            opened_at BIGINT NOT NULL,
            created_at BIGINT NOT NULL
        )
        "#,
        "ALTER TABLE aigc_flags ADD COLUMN IF NOT EXISTS voter_id TEXT",
        "ALTER TABLE aigc_flags ADD COLUMN IF NOT EXISTS voter_name TEXT",
        "ALTER TABLE aigc_flags ADD COLUMN IF NOT EXISTS voter_url_token TEXT",
        "ALTER TABLE aigc_flags ADD COLUMN IF NOT EXISTS voter_avatar_url TEXT",
        "ALTER TABLE aigc_flags ADD COLUMN IF NOT EXISTS credit_bypassed BOOLEAN NOT NULL DEFAULT FALSE",
        "UPDATE aigc_flags SET voter_id = client_id WHERE voter_id IS NULL OR voter_id = ''",
        "UPDATE aigc_flags SET voter_name = client_id WHERE voter_name IS NULL OR voter_name = ''",
        "ALTER TABLE aigc_flags ALTER COLUMN voter_id SET NOT NULL",
        "ALTER TABLE aigc_flags ALTER COLUMN voter_name SET NOT NULL",
        r#"
        DO $$
        DECLARE
            existing_constraint TEXT;
        BEGIN
            SELECT conname INTO existing_constraint
            FROM pg_constraint
            WHERE conrelid = 'aigc_flags'::regclass
              AND contype = 'u'
              AND pg_get_constraintdef(oid) = 'UNIQUE (client_id, content_type, content_id)';

            IF existing_constraint IS NOT NULL THEN
                EXECUTE format('ALTER TABLE aigc_flags DROP CONSTRAINT %I', existing_constraint);
            END IF;
        END $$;
        "#,
        "CREATE UNIQUE INDEX IF NOT EXISTS index_aigc_flags_named_vote ON aigc_flags(content_type, content_id, voter_id)",
        "CREATE INDEX IF NOT EXISTS index_aigc_flags_content ON aigc_flags(content_type, content_id)",
        "CREATE INDEX IF NOT EXISTS index_aigc_flags_content_hash ON aigc_flags(content_type, content_id, content_hash)",
    ];

    for statement in statements {
        sqlx::query(statement).execute(pool).await?;
    }
    Ok(())
}

async fn ensure_client_tx(
    tx: &mut Transaction<'_, Postgres>,
    client_id: &str,
) -> Result<(), ServiceError> {
    let now = now_epoch_seconds();
    sqlx::query(
        r#"
        INSERT INTO clients (id, credit, progress, created_at, last_seen_at)
        VALUES ($1, $2, 0, $3, $3)
        ON CONFLICT(id) DO UPDATE SET last_seen_at = EXCLUDED.last_seen_at
        "#,
    )
    .bind(client_id)
    .bind(INITIAL_CREDIT)
    .bind(now)
    .execute(&mut **tx)
    .await?;
    Ok(())
}

async fn client_account_tx(
    tx: &mut Transaction<'_, Postgres>,
    client_id: &str,
    lock: bool,
) -> Result<ClientAccount, ServiceError> {
    let sql = if lock {
        "SELECT credit, progress FROM clients WHERE id = $1 FOR UPDATE"
    } else {
        "SELECT credit, progress FROM clients WHERE id = $1"
    };
    let row = sqlx::query(sql)
        .bind(client_id)
        .fetch_one(&mut **tx)
        .await?;
    Ok(ClientAccount {
        credit: row.try_get("credit")?,
        progress: row.try_get("progress")?,
    })
}

async fn apply_credit_progress_tx(
    tx: &mut Transaction<'_, Postgres>,
    client_id: &str,
    accepted_events: i64,
) -> Result<(), ServiceError> {
    if accepted_events <= 0 {
        return Ok(());
    }

    let mut account = client_account_tx(tx, client_id, true).await?;
    apply_credit_progress_to_account(&mut account, accepted_events);

    sqlx::query("UPDATE clients SET credit = $1, progress = $2, last_seen_at = $3 WHERE id = $4")
        .bind(account.credit)
        .bind(account.progress)
        .bind(now_epoch_seconds())
        .bind(client_id)
        .execute(&mut **tx)
        .await?;
    Ok(())
}

async fn upsert_content_snapshot_tx(
    tx: &mut Transaction<'_, Postgres>,
    content_type: &str,
    content_id: &str,
    content_hash: &str,
    request: &AigcFlagRequest,
) -> Result<(), ServiceError> {
    upsert_content_snapshot_fields_tx(
        tx,
        content_type,
        content_id,
        content_hash,
        request.content_updated_at,
        &request.title,
        &request.author_hash,
        &request.content_html,
    )
    .await
}

async fn upsert_content_snapshot_fields_tx(
    tx: &mut Transaction<'_, Postgres>,
    content_type: &str,
    content_id: &str,
    content_hash: &str,
    content_updated_at: i64,
    title: &str,
    author_hash: &str,
    content_html: &str,
) -> Result<(), ServiceError> {
    sqlx::query(
        r#"
        INSERT INTO content_snapshots (
            content_type,
            content_id,
            content_hash,
            content_updated_at,
            title,
            author_hash,
            content_html,
            first_seen_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        ON CONFLICT(content_type, content_id, content_hash) DO UPDATE SET
            content_updated_at = GREATEST(
                content_snapshots.content_updated_at,
                EXCLUDED.content_updated_at
            ),
            title = COALESCE(NULLIF(EXCLUDED.title, ''), content_snapshots.title),
            author_hash = COALESCE(NULLIF(EXCLUDED.author_hash, ''), content_snapshots.author_hash),
            content_html = EXCLUDED.content_html
        "#,
    )
    .bind(content_type)
    .bind(content_id)
    .bind(content_hash)
    .bind(content_updated_at)
    .bind(title)
    .bind(author_hash)
    .bind(content_html)
    .bind(now_epoch_seconds())
    .execute(&mut **tx)
    .await?;
    Ok(())
}

async fn build_flag_response_tx(
    tx: &mut Transaction<'_, Postgres>,
    client_id: &str,
    content_type: &str,
    content_id: &str,
    content_hash: &str,
    content_updated_at: i64,
    credit_bypass_available: bool,
) -> Result<AigcFlagResponse, ServiceError> {
    let account = client_account_tx(tx, client_id, false).await?;
    let count = flag_count_tx(tx, content_type, content_id).await?;
    let voters = flag_voters_tx(tx, content_type, content_id).await?;
    let current_version_count: i64 = sqlx::query_scalar(
        r#"
        SELECT COUNT(*)::BIGINT
        FROM aigc_flags
        WHERE content_type = $1 AND content_id = $2 AND content_hash = $3
        "#,
    )
    .bind(content_type)
    .bind(content_id)
    .bind(content_hash)
    .fetch_one(&mut **tx)
    .await?;

    Ok(AigcFlagResponse {
        my_flagged: true,
        credit: account.credit,
        credit_bypass_available,
        effective_flag_count: count,
        raw_flag_count: count,
        current_version_flag_count: current_version_count,
        content_hash: content_hash.to_string(),
        content_updated_at,
        confidence: confidence_for_count(count),
        voters,
        external_source: None,
    })
}

async fn flag_count_tx(
    tx: &mut Transaction<'_, Postgres>,
    content_type: &str,
    content_id: &str,
) -> Result<i64, ServiceError> {
    Ok(sqlx::query_scalar(
        "SELECT COUNT(*)::BIGINT FROM aigc_flags WHERE content_type = $1 AND content_id = $2",
    )
    .bind(content_type)
    .bind(content_id)
    .fetch_one(&mut **tx)
    .await?)
}

async fn has_existing_flag_tx(
    tx: &mut Transaction<'_, Postgres>,
    voter_id: &str,
    content_type: &str,
    content_id: &str,
) -> Result<bool, ServiceError> {
    let value: Option<i64> = sqlx::query_scalar(
        r#"
        SELECT 1::BIGINT
        FROM aigc_flags
        WHERE voter_id = $1 AND content_type = $2 AND content_id = $3
        LIMIT 1
        "#,
    )
    .bind(voter_id)
    .bind(content_type)
    .bind(content_id)
    .fetch_optional(&mut **tx)
    .await?;
    Ok(value.is_some())
}

async fn flag_voters_tx(
    tx: &mut Transaction<'_, Postgres>,
    content_type: &str,
    content_id: &str,
) -> Result<Vec<AigcFlagVoter>, ServiceError> {
    let rows = sqlx::query(
        r#"
        SELECT voter_id, voter_name, voter_url_token, voter_avatar_url, created_at, credit_bypassed
        FROM aigc_flags
        WHERE content_type = $1 AND content_id = $2
        ORDER BY created_at DESC
        LIMIT 10
        "#,
    )
    .bind(content_type)
    .bind(content_id)
    .fetch_all(&mut **tx)
    .await?;

    rows.into_iter()
        .map(|row| {
            Ok(AigcFlagVoter {
                voter_id: row.try_get("voter_id")?,
                voter_name: row.try_get("voter_name")?,
                voter_url_token: row.try_get("voter_url_token")?,
                voter_avatar_url: row.try_get("voter_avatar_url")?,
                created_at: row.try_get("created_at")?,
                credit_bypassed: row.try_get("credit_bypassed")?,
            })
        })
        .collect()
}

/// 从 zhihuai.sx349.xyz 解析到的单条内容投票统计。
#[derive(Debug, Clone, Deserialize, Serialize)]
struct ExternalAigcContentStats {
    #[serde(default)]
    content_type: String,
    #[serde(default)]
    content_id: String,
    #[serde(default)]
    total_votes: i64,
    #[serde(default)]
    voter_count: i64,
    #[serde(default)]
    total_downvotes: i64,
    #[serde(default)]
    downvoter_count: i64,
}

/// 从 zhihuai.sx349.xyz 榜单文件解析到的内容列表。
#[derive(Debug, Deserialize)]
struct ExternalAigcLeaderboard {
    #[serde(default)]
    content: Vec<ExternalAigcContentStats>,
}

/// 每次查询同步刷新当前内容的外部源；榜单缓存放到后台刷新，避免拖慢 App 展示。
async fn refresh_and_load_external_aigc_stats(
    pool: &PgPool,
    content_type: &str,
    content_id: &str,
) -> Option<ExternalAigcSource> {
    match refresh_external_aigc_stats(pool, content_type, content_id).await {
        Ok(Some(source)) => Some(source),
        _ => cached_external_aigc_stats(pool, content_type, content_id)
            .await
            .ok()
            .flatten(),
    }
}

/// 刷新当前内容的 zhihuai 统计，同时异步缓存 24 小时榜和历史总榜数据。
async fn refresh_external_aigc_stats(
    pool: &PgPool,
    content_type: &str,
    content_id: &str,
) -> Result<Option<ExternalAigcSource>, ServiceError> {
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(EXTERNAL_AIGC_TIMEOUT_SECONDS))
        .build()
        .map_err(|error| ServiceError::internal(error.to_string()))?;
    let refreshed_at = now_epoch_seconds();
    let mut current_source = None;

    if let Ok(stats) = fetch_external_aigc_content_stats(&client, content_type, content_id).await {
        let source = stats.to_source_with_identity(content_type, content_id, refreshed_at);
        upsert_external_aigc_cache(pool, &source, &stats).await?;
        current_source = Some(source);
    }

    spawn_external_aigc_leaderboard_cache_refresh(pool.clone(), client, refreshed_at);

    Ok(current_source)
}

/// 后台刷新 zhihuai 24 小时榜和历史总榜缓存；失败不影响当前内容投票状态返回。
fn spawn_external_aigc_leaderboard_cache_refresh(
    pool: PgPool,
    client: reqwest::Client,
    refreshed_at: i64,
) {
    let last_attempt = EXTERNAL_AIGC_LEADERBOARD_LAST_REFRESH_ATTEMPT_AT.load(Ordering::Relaxed);
    if refreshed_at - last_attempt < EXTERNAL_AIGC_LEADERBOARD_REFRESH_INTERVAL_SECONDS {
        return;
    }
    if EXTERNAL_AIGC_LEADERBOARD_REFRESH_IN_FLIGHT
        .compare_exchange(false, true, Ordering::Acquire, Ordering::Relaxed)
        .is_err()
    {
        return;
    }
    EXTERNAL_AIGC_LEADERBOARD_LAST_REFRESH_ATTEMPT_AT.store(refreshed_at, Ordering::Relaxed);
    tokio::spawn(async move {
        let _ = refresh_external_aigc_leaderboard_cache(&pool, &client, refreshed_at).await;
        EXTERNAL_AIGC_LEADERBOARD_REFRESH_IN_FLIGHT.store(false, Ordering::Release);
    });
}

/// 将 zhihuai 榜单批量写入缓存表；调用方负责决定是否阻塞当前请求。
async fn refresh_external_aigc_leaderboard_cache(
    pool: &PgPool,
    client: &reqwest::Client,
    refreshed_at: i64,
) -> Result<(), ServiceError> {
    for path in ["/data/rolling24h.json", "/data/alltime.json"] {
        if let Ok(leaderboard) = fetch_external_aigc_leaderboard(&client, path).await {
            for stats in leaderboard.content {
                if stats.content_type.is_blank() || stats.content_id.is_blank() {
                    continue;
                }
                let source = stats.to_source(refreshed_at);
                upsert_external_aigc_cache(pool, &source, &stats).await?;
            }
        }
    }

    Ok(())
}

/// 调用 zhihuai resolve 接口，获取当前内容的外部投票统计。
async fn fetch_external_aigc_content_stats(
    client: &reqwest::Client,
    content_type: &str,
    content_id: &str,
) -> Result<ExternalAigcContentStats, ServiceError> {
    let response = client
        .get(format!("{EXTERNAL_AIGC_BASE_URL}/api/v1/resolve"))
        .query(&[("content_type", content_type), ("content_id", content_id)])
        .send()
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))?;
    if !response.status().is_success() {
        return Err(ServiceError::internal(format!(
            "external AIGC source returned {}",
            response.status()
        )));
    }
    response
        .json()
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))
}

/// 读取 zhihuai 的榜单 JSON，用于把榜单内容批量写入本地缓存。
async fn fetch_external_aigc_leaderboard(
    client: &reqwest::Client,
    path: &str,
) -> Result<ExternalAigcLeaderboard, ServiceError> {
    let response = client
        .get(format!("{EXTERNAL_AIGC_BASE_URL}{path}"))
        .send()
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))?;
    if !response.status().is_success() {
        return Err(ServiceError::internal(format!(
            "external AIGC leaderboard returned {}",
            response.status()
        )));
    }
    response
        .json()
        .await
        .map_err(|error| ServiceError::internal(error.to_string()))
}

/// 将 zhihuai 当前内容或榜单统计写入本地缓存表。
async fn upsert_external_aigc_cache(
    pool: &PgPool,
    source: &ExternalAigcSource,
    raw: &ExternalAigcContentStats,
) -> Result<(), ServiceError> {
    sqlx::query(
        r#"
        INSERT INTO external_aigc_content_cache (
            source,
            content_type,
            content_id,
            total_votes,
            voter_count,
            total_downvotes,
            downvoter_count,
            refreshed_at,
            raw_json
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9::jsonb)
        ON CONFLICT(source, content_type, content_id) DO UPDATE SET
            total_votes = EXCLUDED.total_votes,
            voter_count = EXCLUDED.voter_count,
            total_downvotes = EXCLUDED.total_downvotes,
            downvoter_count = EXCLUDED.downvoter_count,
            refreshed_at = EXCLUDED.refreshed_at,
            raw_json = EXCLUDED.raw_json
        "#,
    )
    .bind(&source.source)
    .bind(&source.content_type)
    .bind(&source.content_id)
    .bind(source.total_votes)
    .bind(source.voter_count)
    .bind(source.total_downvotes)
    .bind(source.downvoter_count)
    .bind(source.refreshed_at)
    .bind(serde_json::to_string(raw).map_err(|error| ServiceError::internal(error.to_string()))?)
    .execute(pool)
    .await?;
    Ok(())
}

/// 外部源刷新失败时，从本地缓存读取最近一次 zhihuai 统计。
async fn cached_external_aigc_stats(
    pool: &PgPool,
    content_type: &str,
    content_id: &str,
) -> Result<Option<ExternalAigcSource>, ServiceError> {
    let row = sqlx::query(
        r#"
        SELECT
            source,
            content_type,
            content_id,
            total_votes,
            voter_count,
            total_downvotes,
            downvoter_count,
            refreshed_at
        FROM external_aigc_content_cache
        WHERE source = $1 AND content_type = $2 AND content_id = $3
        "#,
    )
    .bind(EXTERNAL_AIGC_SOURCE)
    .bind(content_type)
    .bind(content_id)
    .fetch_optional(pool)
    .await?;

    row.map(|row| {
        Ok(ExternalAigcSource {
            source: row.try_get("source")?,
            content_type: row.try_get("content_type")?,
            content_id: row.try_get("content_id")?,
            total_votes: row.try_get("total_votes")?,
            voter_count: row.try_get("voter_count")?,
            total_downvotes: row.try_get("total_downvotes")?,
            downvoter_count: row.try_get("downvoter_count")?,
            refreshed_at: row.try_get("refreshed_at")?,
        })
    })
    .transpose()
}

impl ExternalAigcContentStats {
    /// 将榜单中的 zhihuai 统计转换成服务端响应使用的外部源结构。
    fn to_source(&self, refreshed_at: i64) -> ExternalAigcSource {
        self.to_source_with_identity(&self.content_type, &self.content_id, refreshed_at)
    }

    /// 将 zhihuai 统计转换成服务端响应结构；resolve 接口会从路径参数补齐内容标识。
    fn to_source_with_identity(
        &self,
        content_type: &str,
        content_id: &str,
        refreshed_at: i64,
    ) -> ExternalAigcSource {
        ExternalAigcSource {
            source: EXTERNAL_AIGC_SOURCE.to_string(),
            content_type: content_type.to_string(),
            content_id: content_id.to_string(),
            total_votes: self.total_votes,
            voter_count: self.voter_count,
            total_downvotes: self.total_downvotes,
            downvoter_count: self.downvoter_count,
            refreshed_at,
        }
    }
}

fn apply_credit_progress_to_account(account: &mut ClientAccount, accepted_events: i64) {
    if account.credit >= CREDIT_CAP {
        account.progress = 0;
        return;
    }

    let total_progress = account.progress + accepted_events;
    let award = (total_progress / READS_PER_CREDIT).min(CREDIT_CAP - account.credit);
    account.credit += award;
    account.progress = if account.credit >= CREDIT_CAP {
        0
    } else {
        total_progress % READS_PER_CREDIT
    };
}

fn validate_client_id(client_id: &str) -> Result<(), ServiceError> {
    if client_id.is_blank() {
        return Err(ServiceError::bad_request("client_id is required"));
    }
    Ok(())
}

fn validate_content_identity(content_type: &str, content_id: &str) -> Result<(), ServiceError> {
    if !matches!(content_type, "answer" | "article") {
        return Err(ServiceError::bad_request(
            "content_type must be answer or article",
        ));
    }
    if content_id.is_blank() {
        return Err(ServiceError::bad_request("content_id is required"));
    }
    Ok(())
}

fn validate_flag_request(request: &AigcFlagRequest) -> Result<(), ServiceError> {
    validate_voter_identity(&request.voter)?;
    if request.content_html.is_blank() {
        return Err(ServiceError::bad_request("content_html is required"));
    }
    if request.content_updated_at <= 0 {
        return Err(ServiceError::bad_request("content_updated_at is required"));
    }
    Ok(())
}

fn validate_flag_evidence(request: &AigcFlagRequest) -> Result<(), ServiceError> {
    if request.evidence.client_view_duration_ms < MIN_FLAG_DURATION_MS
        || request.evidence.scroll_depth < MIN_FLAG_SCROLL_DEPTH
    {
        return Err(ServiceError::bad_request(
            "flag evidence does not look like a real read",
        ));
    }
    Ok(())
}

fn validate_voter_identity(voter: &VoterIdentity) -> Result<(), ServiceError> {
    if voter.id.is_blank() {
        return Err(ServiceError::bad_request("voter.id is required"));
    }
    if voter.name.is_blank() {
        return Err(ServiceError::bad_request("voter.name is required"));
    }
    Ok(())
}

fn is_valid_read_event(event: &ReadEvent) -> bool {
    matches!(event.content_type.as_str(), "answer" | "article")
        && !event.content_id.is_blank()
        && !event.content_html.is_blank()
        && event.content_updated_at > 0
        && event.opened_at > 0
        && event.foreground_duration_ms >= MIN_READ_DURATION_MS
        && event.max_scroll_ratio >= MIN_READ_SCROLL_RATIO
}

fn sha256_hex(value: &str) -> String {
    let digest = Sha256::digest(value.as_bytes());
    format!("{digest:x}")
}

fn confidence_for_count(count: i64) -> String {
    match count {
        0..=2 => "low",
        3..=9 => "medium",
        _ => "high",
    }
    .to_string()
}

fn normalize_identity_tokens(tokens: HashSet<String>) -> HashSet<String> {
    tokens
        .into_iter()
        .filter_map(|token| normalize_identity_token(&token))
        .collect()
}

fn normalize_identity_token(token: &str) -> Option<String> {
    let trimmed = token.trim();
    if trimmed.is_empty() {
        None
    } else {
        Some(trimmed.to_lowercase())
    }
}

fn has_credit_bypass_token(tokens: &HashSet<String>, token: &str) -> bool {
    normalize_identity_token(token).is_some_and(|token| tokens.contains(&token))
}

fn is_credit_bypass_voter(
    tokens: &HashSet<String>,
    client_id: &str,
    voter: &VoterIdentity,
) -> bool {
    has_credit_bypass_token(tokens, client_id)
        || has_credit_bypass_token(tokens, &voter.id)
        || has_credit_bypass_token(tokens, &voter.name)
        || voter
            .url_token
            .as_deref()
            .is_some_and(|url_token| has_credit_bypass_token(tokens, url_token))
}

fn is_credit_bypass_status_query(
    tokens: &HashSet<String>,
    client_id: &str,
    voter_id: &str,
    voter_name: &str,
    voter_url_token: &str,
) -> bool {
    has_credit_bypass_token(tokens, client_id)
        || has_credit_bypass_token(tokens, voter_id)
        || has_credit_bypass_token(tokens, voter_name)
        || has_credit_bypass_token(tokens, voter_url_token)
}

fn flag_voter_from_identity(
    voter: &VoterIdentity,
    created_at: i64,
    credit_bypassed: bool,
) -> AigcFlagVoter {
    AigcFlagVoter {
        voter_id: voter.id.clone(),
        voter_name: voter.name.clone(),
        voter_url_token: voter.url_token.clone(),
        voter_avatar_url: voter.avatar_url.clone(),
        created_at,
        credit_bypassed,
    }
}

fn now_epoch_seconds() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_secs() as i64)
        .unwrap_or_default()
}

#[derive(Debug, Clone, Copy)]
struct ClientAccount {
    credit: i64,
    progress: i64,
}

trait BlankStr {
    fn is_blank(&self) -> bool;
}

impl BlankStr for str {
    fn is_blank(&self) -> bool {
        self.trim().is_empty()
    }
}
