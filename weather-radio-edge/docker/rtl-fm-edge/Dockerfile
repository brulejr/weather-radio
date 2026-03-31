FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        rtl-sdr \
        socat \
        netcat-openbsd \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY docker/rtl-fm-edge/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]