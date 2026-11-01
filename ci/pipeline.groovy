// Scripted pipeline body, load()ed by the bootstrap job defined in
// jenkins/casc.yaml. Runs inside a node{} block with the repo already synced
// into the workspace, so edits here take effect on the next build.

def NET = 'ci-net'
def SERVICES = ['py-service', 'java-service']

stage('Build images') {
    for (svc in SERVICES) {
        sh "docker build -t ${svc}:ci services/${svc}"
    }
}

stage('Run services') {
    sh "docker network create ${NET} || true"
    // READY_DELAY_SECONDS=3 makes readiness genuinely lag liveness, so the
    // robot polling keyword has something real to wait for.
    for (svc in SERVICES) {
        sh "docker rm -f ${svc} || true"
        sh "docker run -d --name ${svc} --network ${NET} -e READY_DELAY_SECONDS=3 ${svc}:ci"
    }
}

try {
    stage('Robot tests') {
        // Runs in a throwaway uv container on the same Docker network, so the
        // tests reach the services by container name. The robot/ dir is
        // mounted from the HOST path (the docker daemon is the host's — see
        // docker-compose.yml). --user 1000 keeps result files owned by fred,
        // and the venv/cache land in /tmp inside the container, not the repo.
        sh """
            docker run --rm --network ${NET} \\
                --user 1000:1000 \\
                -e HOME=/tmp -e UV_CACHE_DIR=/tmp/uv-cache -e UV_PROJECT_ENVIRONMENT=/tmp/venv \\
                -v ${env.HOST_REPO}/robot:/robot -w /robot \\
                ghcr.io/astral-sh/uv:python3.12-bookworm-slim \\
                uv run --frozen robot \\
                    --variable PY_URL:http://py-service:8000 \\
                    --variable JAVA_URL:http://java-service:8080 \\
                    --xunit xunit.xml \\
                    --outputdir results \\
                    tests
        """
    }
} finally {
    stage('Collect results') {
        // Results were written to the host repo's robot/results; the Jenkins
        // container sees that same dir read-only at /workspace.
        sh 'rm -rf robot-results && cp -r /workspace/robot/results robot-results || true'
        if (fileExists('robot-results/xunit.xml')) {
            junit 'robot-results/xunit.xml'
        }
        archiveArtifacts artifacts: 'robot-results/**', allowEmptyArchive: true
    }
    stage('Teardown') {
        for (svc in SERVICES) {
            sh "docker rm -f ${svc} || true"
        }
    }
}
