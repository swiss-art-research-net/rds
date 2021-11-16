# RDS
SARI Reference Data Services

## Platform Setup

1. Clone the [Metaphacts Docker Compose](https://github.com/metaphacts/metaphactory-docker-compose) setup
1. Copy the `service-template` directory in the Docker Compose setup
    1. `cp -r service-template rds-global`
1. Create an `.env` file in your `rds-global` directory by copying it from the provided examples.
    1. `cp database-config/.env_blazegraph .env`
1. Edit the obtained `.env` file:
    1. `COMPOSE_PROJECT_NAME=rds-global`
    1. `HOST_NAME=swissartresearch.net`
    1. `METAPHACTORY_IMAGE=swissartresearx/rds-global:latest`
1. Edit the `docker-compose.overwrite.yml` file. See below for an example:
    1. ```version: "2.2"
        services:
            metaphactory:
                networks:
                - default
                mem_limit: 4g
                volumes:
                - /mnt/rds-global/runtime-data:/runtime-data:Z
                ports:
                - "10214:8080"
            blazegraph:
                ports:
                - 8080:8080
                volumes:
                - /mnt/rds-global/blazegraph-data:/blazegraph-data:Z
                environment:
                - JAVA_OPTS=-Xmx16g
                - QUERY_TIMEOUT=0

        networks:
            default:
                external:
                name: nginx_proxy_network
        ```
1. In order to update the security configuration, it might be necessary to set the `securityConfigStorageId` environment parameter by editing the `.env` file:
    1. `METAPHACTORY_OPTS=-Dconfig.storage.rds-global.type=nonVersionedFile -Dconfig.storage.rds-global.mutable=true -Dconfig.storage.rds-global.root=/rdsapps/rds-global`

## Data Preparation

Follow the separate [instructions for data preparation](https://github.com/swiss-art-research-net/rds/tree/main/data-preparation#readme) for populating the database.