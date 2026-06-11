#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/deploy-off.sh [--reset-data] [--skip-smoke] [--build-only]

Deploy aigc-vote-server to the off host.

Options:
  --reset-data   Drop AIGC vote tables before starting the new container.
                 This discards old vote/read/snapshot/client data and lets the
                 service recreate schema from the current binary.
  --skip-smoke   Skip the read-event/database smoke test.
  --build-only   Build the Linux binary on off and exit without restarting the container.

Environment overrides:
  OFF_HOST                         default: off
  AIGC_VOTE_REMOTE_BASE            default: /home/dom/services/aigc-vote
  AIGC_VOTE_IMAGE                  default: aigc-vote-server:off
  AIGC_VOTE_CONTAINER              default: aigc-vote-server
  AIGC_VOTE_ENV_FILE               default: /home/dom/services/aigc-vote/aigc-vote.env
  AIGC_VOTE_POSTGRES_CONTAINER     default: postgres-office
  AIGC_VOTE_LOCAL_HEALTH_URL       default: http://127.0.0.1:18787/healthz
  AIGC_VOTE_PUBLIC_HEALTH_URL      default: https://aigc-vote.ai.fintechedu.cn/healthz
  AIGC_VOTE_REMOTE_CARGO_JOBS      default: 2
  AIGC_VOTE_REMOTE_PROXY           default: http://127.0.0.1:7890
USAGE
}

RESET_DATA=0
SKIP_SMOKE=0
BUILD_ONLY=0

while (($# > 0)); do
  case "$1" in
    --reset-data)
      RESET_DATA=1
      ;;
    --skip-smoke)
      SKIP_SMOKE=1
      ;;
    --build-only)
      BUILD_ONLY=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"

OFF_HOST="${OFF_HOST:-off}"
REMOTE_BASE="${AIGC_VOTE_REMOTE_BASE:-/home/dom/services/aigc-vote}"
REMOTE_SRC="$REMOTE_BASE/src"
REMOTE_BUILD="$REMOTE_BASE/runtime-build"
IMAGE="${AIGC_VOTE_IMAGE:-aigc-vote-server:off}"
CONTAINER="${AIGC_VOTE_CONTAINER:-aigc-vote-server}"
ENV_FILE="${AIGC_VOTE_ENV_FILE:-$REMOTE_BASE/aigc-vote.env}"
POSTGRES_CONTAINER="${AIGC_VOTE_POSTGRES_CONTAINER:-postgres-office}"
LOCAL_HEALTH_URL="${AIGC_VOTE_LOCAL_HEALTH_URL:-http://127.0.0.1:18787/healthz}"
PUBLIC_HEALTH_URL="${AIGC_VOTE_PUBLIC_HEALTH_URL:-https://aigc-vote.ai.fintechedu.cn/healthz}"
CARGO_JOBS="${AIGC_VOTE_REMOTE_CARGO_JOBS:-2}"
REMOTE_PROXY="${AIGC_VOTE_REMOTE_PROXY:-http://127.0.0.1:7890}"
BINARY_NAME="aigc-vote-server-linux-amd64"

log() {
  printf '[deploy-off] %s\n' "$*"
}

ssh_off() {
  ssh "$OFF_HOST" "$@"
}

log "preflight"
ssh_off "set -e
test -r '$ENV_FILE'
sudo -n true
sudo -n docker ps >/dev/null
command -v gcc >/dev/null
test -x \"\$HOME/.cargo/bin/cargo\"
mkdir -p '$REMOTE_SRC' '$REMOTE_BUILD'
"

log "sync source to $OFF_HOST:$REMOTE_SRC"
rsync -az --delete "$APP_DIR/src/" "$OFF_HOST:$REMOTE_SRC/src/"
rsync -az "$APP_DIR/Cargo.toml" "$APP_DIR/Cargo.lock" "$OFF_HOST:$REMOTE_SRC/"

log "build binary on off with CARGO_BUILD_JOBS=$CARGO_JOBS"
ssh_off "set -e
cd '$REMOTE_SRC'
PATH=\"\$HOME/.cargo/bin:\$PATH\" \
  CARGO_BUILD_JOBS='$CARGO_JOBS' \
  HTTP_PROXY='$REMOTE_PROXY' HTTPS_PROXY='$REMOTE_PROXY' \
  http_proxy='$REMOTE_PROXY' https_proxy='$REMOTE_PROXY' \
  cargo build --release
cp target/release/aigc-vote-server '$REMOTE_BUILD/$BINARY_NAME'
chmod 0755 '$REMOTE_BUILD/$BINARY_NAME'
"

if [[ "$BUILD_ONLY" == "1" ]]; then
  log "built $OFF_HOST:$REMOTE_BUILD/$BINARY_NAME"
  exit 0
fi

log "sync runtime Dockerfile to $OFF_HOST:$REMOTE_BUILD"
rsync -az "$APP_DIR/Dockerfile.runtime" "$OFF_HOST:$REMOTE_BUILD/"

log "build runtime image $IMAGE on off"
ssh_off "cd '$REMOTE_BUILD' && sudo env DOCKER_BUILDKIT=0 docker build --pull=false --network host \
  --build-arg HTTP_PROXY='$REMOTE_PROXY' \
  --build-arg HTTPS_PROXY='$REMOTE_PROXY' \
  --build-arg http_proxy='$REMOTE_PROXY' \
  --build-arg https_proxy='$REMOTE_PROXY' \
  -f Dockerfile.runtime -t '$IMAGE' ."

log "stop old container $CONTAINER"
ssh_off "sudo docker rm -f '$CONTAINER' >/dev/null 2>&1 || true"

if [[ "$RESET_DATA" == "1" ]]; then
  log "drop old AIGC vote data"
  ssh_off \
    "ENV_FILE='$ENV_FILE' POSTGRES_CONTAINER='$POSTGRES_CONTAINER' bash -s" <<'REMOTE_RESET'
set -euo pipefail
set -a
. "$ENV_FILE"
set +a
DB_URL="${AIGC_VOTE_DATABASE_URL/127.0.0.1:15432/127.0.0.1:5432}"
sudo docker exec "$POSTGRES_CONTAINER" psql "$DB_URL" -v ON_ERROR_STOP=1 -c \
  'DROP TABLE IF EXISTS aigc_flags, content_snapshots, read_events, clients CASCADE;'
REMOTE_RESET
fi

log "start container $CONTAINER"
ssh_off "sudo docker run -d --name '$CONTAINER' \
  --network host \
  --env-file '$ENV_FILE' \
  --restart unless-stopped \
  '$IMAGE'"

log "verify local health"
ssh_off "curl -fsS '$LOCAL_HEALTH_URL'"
printf '\n'

log "verify public health"
curl -fsS "$PUBLIC_HEALTH_URL"
printf '\n'

if [[ "$SKIP_SMOKE" != "1" ]]; then
  log "smoke read-event snapshot write"
  ssh_off \
    "ENV_FILE='$ENV_FILE' POSTGRES_CONTAINER='$POSTGRES_CONTAINER' LOCAL_HEALTH_URL='$LOCAL_HEALTH_URL' bash -s" <<'REMOTE_SMOKE'
set -euo pipefail
set -a
. "$ENV_FILE"
set +a

DB_URL="${AIGC_VOTE_DATABASE_URL/127.0.0.1:15432/127.0.0.1:5432}"
ID="deploy-smoke-$(date +%s)"
HTML="<p>deploy smoke ${ID}</p>"
BODY=$(printf '{"client_id":"%s","events":[{"content_type":"answer","content_id":"%s","title":"部署 smoke","author_hash":"deploy-author","content_html":"%s","content_updated_at":1781020000,"opened_at":1781020123,"foreground_duration_ms":45000,"max_scroll_ratio":0.82}]}' "$ID" "$ID" "$HTML")

curl -fsS -X POST "${LOCAL_HEALTH_URL%/healthz}/v1/read-events:batch" \
  -H 'content-type: application/json' \
  --data "$BODY"
printf '\n'

SNAPSHOT_COUNT=$(sudo docker exec "$POSTGRES_CONTAINER" psql "$DB_URL" -v ON_ERROR_STOP=1 -Atc \
  "SELECT COUNT(*) FROM content_snapshots WHERE content_type = 'answer' AND content_id = '$ID' AND content_html = \$\$$HTML\$\$;")
READ_COUNT=$(sudo docker exec "$POSTGRES_CONTAINER" psql "$DB_URL" -v ON_ERROR_STOP=1 -Atc \
  "SELECT COUNT(*) FROM read_events WHERE client_id = '$ID' AND content_type = 'answer' AND content_id = '$ID';")
READ_HTML_COLUMN_COUNT=$(sudo docker exec "$POSTGRES_CONTAINER" psql "$DB_URL" -v ON_ERROR_STOP=1 -Atc \
  "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'read_events' AND column_name = 'content_html';")

sudo docker exec "$POSTGRES_CONTAINER" psql "$DB_URL" -v ON_ERROR_STOP=1 -Atc \
  "DELETE FROM read_events WHERE client_id = '$ID'; DELETE FROM content_snapshots WHERE content_id = '$ID'; DELETE FROM clients WHERE id = '$ID';" >/dev/null

if [[ "$SNAPSHOT_COUNT" != "1" || "$READ_COUNT" != "1" || "$READ_HTML_COLUMN_COUNT" != "0" ]]; then
  echo "smoke failed: snapshot=$SNAPSHOT_COUNT read=$READ_COUNT read_html_column=$READ_HTML_COLUMN_COUNT" >&2
  exit 1
fi
REMOTE_SMOKE
fi

log "done"
