# Jenkins + Robot Framework CI prototype

Jenkins (in Docker) builds two tiny services, runs them on a Docker network,
and runs Robot Framework liveness/readiness tests against them.

## Layout

| Path | What |
|---|---|
| `jenkins/` | Jenkins image (docker CLI + plugins), compose file, JCasC config |
| `ci/pipeline.groovy` | The actual pipeline body (edits apply on next build, no reload) |
| `services/py-service/` | FastAPI service, `/health/live` + `/health/ready`, port 8000 |
| `services/java-service/` | Java 21 JDK-HttpServer service, same endpoints, port 8080 |
| `robot/` | Robot Framework suites + uv project |

Both services take `READY_DELAY_SECONDS` (default 0): liveness answers 200
immediately, readiness returns 503 until the delay elapses — simulated warmup,
so the readiness tests genuinely poll.

## Jenkins

```bash
cd jenkins
cp .env.example .env             # then adjust DOCKER_GID / HOST_REPO for your machine
docker compose up -d --build     # first start takes a few minutes (plugins)
```

- UI: <http://localhost:8080>, login `admin` / `admin`.
- One job, `ci`, is created automatically by `jenkins/casc.yaml` (JCasC + Job DSL).
- Trigger it from the UI ("Build Now") — stages: Sync repo → Build images →
  Run services → Robot tests → Collect results (junit + artifacts) → Teardown.
- Requires Docker Desktop running on the Windows side.

How it works without git: the repo is mounted read-only at `/workspace` in the
Jenkins container; the job's inline script just copies it into the workspace
and `load`s `ci/pipeline.groovy`. The pipeline talks to the **host** docker
daemon via the mounted socket, so service containers and the robot test
container run as siblings of Jenkins on the `ci-net` network. Consequence:
volume paths passed by the pipeline are host paths (`HOST_REPO` in
`jenkins/.env`, along with `DOCKER_GID`, the gid of `/var/run/docker.sock`).

When this becomes a git repo: switch the job to "Pipeline from SCM" with a
root `Jenkinsfile` and delete the bootstrap script in `casc.yaml`.

## Local development (no Jenkins)

```bash
# services, with ports published to the host
docker build -t py-service:ci services/py-service
docker build -t java-service:ci services/java-service
docker run -d --rm --name py-service   -p 8000:8000 -e READY_DELAY_SECONDS=3 py-service:ci
docker run -d --rm --name java-service -p 8081:8080 -e READY_DELAY_SECONDS=3 java-service:ci

# robot tests from the uv venv (defaults target localhost:8000 / localhost:8081)
cd robot
uv sync
uv run robot --outputdir results tests

docker rm -f py-service java-service
```
