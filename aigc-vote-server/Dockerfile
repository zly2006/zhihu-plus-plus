# syntax=docker/dockerfile:1.7

FROM rust:1-bookworm AS builder

WORKDIR /src
COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/usr/local/cargo/git \
    --mount=type=cache,target=/src/target \
    cargo build --release \
    && cp /src/target/release/aigc-vote-server /tmp/aigc-vote-server

FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --user-group --home /nonexistent --shell /usr/sbin/nologin aigcvote

COPY --from=builder /tmp/aigc-vote-server /usr/local/bin/aigc-vote-server

USER aigcvote
ENV AIGC_VOTE_LISTEN=0.0.0.0:8787

CMD ["/usr/local/bin/aigc-vote-server"]
