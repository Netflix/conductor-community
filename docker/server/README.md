# Docker
## Conductor server
This Dockerfile create the conductor:server image

## Building the image

Run the following commands from the project root. This requires redis, postgres and elastic search 7 to be running.

`docker build -f docker/server/Dockerfile -t conductor:server .`

## Building the standalone image

Run the following commands from the project root.
`docker-compose -f docker/docker-compose-postgres.yaml build`

## Running the conductor server
Run the following commands from the project root.
`docker-compose -f docker/docker-compose-postgres.yaml up`
