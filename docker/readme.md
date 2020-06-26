# Build Webapp Image

In the `webapp/target` directory run `docker build -t aurambaj/mojito-webapp-spring2 -f ../src/main/docker/Dockerfile .`

To test run it: `docker run -p 8080:8080 aurambaj/mojito-webapp-spring2:latest`

# Docker compose

You first have to build the webapp docker image locally as it is not pushed on docker hub. It uses
`aurambaj/mojito-webapp-spring2` because when using `aurambaj/mojito-webapp` the latest local image is not used 
by `docker-compose` (instead it uses the version on hub). This needs review, but that is good enought to get started.

When those pre-requisites are met, just run: `docker-compose up`

# Find IP on Mac

If you running on Mac, `localhost` may not work. To get the IP to reach the service: `docker-machine ip default`.

`export l10n_resttemplate_host="192.168.99.111" ` and then whatever CLI command you have to ru, eg. 
`mojito demo-create -n dockertest`