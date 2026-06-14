use std::env;
use std::net::SocketAddr;

use aigc_vote_server::{AppState, parse_credit_bypass_voters, serve};

#[tokio::main]
async fn main() {
    let database_url =
        env::var("AIGC_VOTE_DATABASE_URL").expect("AIGC_VOTE_DATABASE_URL must be configured");
    let credit_bypass_voters =
        parse_credit_bypass_voters(&env::var("AIGC_VOTE_CREDIT_BYPASS_VOTERS").unwrap_or_default());
    let listen_addr = env::var("AIGC_VOTE_LISTEN")
        .unwrap_or_else(|_| "127.0.0.1:8787".to_string())
        .parse::<SocketAddr>()
        .expect("AIGC_VOTE_LISTEN must be a socket address");

    let state =
        AppState::new_postgres_with_credit_bypass_voters(&database_url, credit_bypass_voters)
            .await
            .expect("initialize database");
    serve(state, listen_addr)
        .await
        .expect("serve AIGC vote API");
}
