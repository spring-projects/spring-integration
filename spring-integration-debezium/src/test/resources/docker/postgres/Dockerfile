# When changed, update also build-all-multiplatform.sh to make
# sure give debezim/postgres version is built also for ARM.
FROM quay.io/debezium/postgres:15

LABEL maintainer="Debezium Community"

COPY inventory.sql /docker-entrypoint-initdb.d/