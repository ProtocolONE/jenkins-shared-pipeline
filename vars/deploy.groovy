def call() {
    echo "Start Deploy"

    script {
        withCredentials([string(credentialsId: 'K8S_CI_TOKEN', variable: 'K8S_CI_TOKEN')]) {
            sh '''
                echo $P1_PROJECT && echo $CI_REGISTRY_IMAGE && echo $BUILD_ID &&
                docker run \
                --rm \
                -v $PWD/.helm:/.helm \
                -e "K8S_API_URL=$K8S_API_URL" \
                -e "K8S_CI_TOKEN=$K8S_CI_TOKEN" \
                -e "P1_PROJECT=$P1_PROJECT" \
                -e "CI_REGISTRY_IMAGE=$CI_REGISTRY_IMAGE" \
                -e NGX_IMAGE=`if [ -f Dockerfile.nginx ]; then echo $CI_REGISTRY_IMAGE ; else echo nginx; fi` \
                p1hub/kubernetes-helm:2.11.0 \
                /bin/sh -c \
                'kubectl config set-cluster k8s --insecure-skip-tls-verify=true --server=$K8S_API_URL &&
                kubectl config set-credentials ci --token=$K8S_CI_TOKEN &&
                kubectl config set-context ci --cluster=k8s --user=ci &&
                kubectl config use-context ci &&
                helm init --client-only &&
                helm upgrade --install $P1_PROJECT .helm \
                --set backend.image=$CI_REGISTRY_IMAGE \
                --set backend.imageTag=$BUILD_ID \
                --set frontend.image=$NGX_IMAGE \
                --set frontend.imageTag="$BUILD_ID"-static \
                --debug \
                --wait \
                --dry-run \
                --timeout 180 ||
                (helm history --max 2 $P1_PROJECT | head -n 2 | tail -n 1 | awk "{print \$1}" | xargs helm rollback $P1_PROJECT && exit 1)'
                '''
            }
        }
}
