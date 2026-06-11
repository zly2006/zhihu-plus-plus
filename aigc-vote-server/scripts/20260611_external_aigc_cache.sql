CREATE TABLE IF NOT EXISTS external_aigc_content_cache (
    source TEXT NOT NULL,
    content_type TEXT NOT NULL,
    content_id TEXT NOT NULL,
    total_votes BIGINT NOT NULL DEFAULT 0,
    voter_count BIGINT NOT NULL DEFAULT 0,
    total_downvotes BIGINT NOT NULL DEFAULT 0,
    downvoter_count BIGINT NOT NULL DEFAULT 0,
    refreshed_at BIGINT NOT NULL,
    raw_json JSONB NOT NULL,
    PRIMARY KEY (source, content_type, content_id)
);

CREATE INDEX IF NOT EXISTS index_external_aigc_content_cache_refreshed_at
    ON external_aigc_content_cache(source, refreshed_at DESC);

