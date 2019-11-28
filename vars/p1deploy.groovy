def call(devBranch = "", devNameSpace = "",ingressPrefix="dev-") {
    echo "Start Deploy"

    script {
        withCredentials([string(credentialsId: 'K8S_CI_TOKEN', variable: 'K8S_CI_TOKEN')]) {
            if(params.PROD_RELEASE){
                sh ''' echo "production release"'''              
            } else {
                sh ''' echo "test release"'''
            }
            devBranch=devBranch.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
            
            helmRelease = env.P1_PROJECT
            k8sNameSpace="default"
            k8sIngressPrefix=""
            BR_NAME=env.BRANCH_NAME
            BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
            helmDebug="--debug"

            if(devBranch!="" && devBranch==BR_NAME){
                k8sNameSpace=devNameSpace
                k8sIngressPrefix=ingressPrefix
                
                helmRelease="${env.P1_PROJECT}-${BR_NAME}"
                //helmDebug="--debug --dry-run"
            }
            sh "echo branch: ${BR_NAME} helm release: ${helmRelease}"


            sh """
                export NGX_IMAGE=`if [ -f Dockerfile.nginx ]; then echo \$CI_REGISTRY_IMAGE ; else echo nginx; fi`
                export HELM_DIR=`[ -d deployments/helm ] && echo "./deployments/helm" || echo ".helm"`
                echo "Helm dir: \$HELM_DIR"
                docker run \
                --rm \
                -v \$PWD/\$HELM_DIR:/.helm \
                -e "K8S_API_URL=\$K8S_API_URL" \
                -e "K8S_CI_TOKEN=\$K8S_CI_TOKEN" \
                -e "P1_PROJECT=\$P1_PROJECT" \
                -e "CI_REGISTRY_IMAGE=\$CI_REGISTRY_IMAGE" \
                -e "BUILD_ID=\$BUILD_ID" \
                -e NGX_IMAGE=`if [ -f Dockerfile.nginx ]; then echo \$CI_REGISTRY_IMAGE ; else echo nginx; fi` \
                p1hub/kubernetes-helm:2.11.0 \
                /bin/sh -c \
                'kubectl config set-cluster k8s --insecure-skip-tls-verify=true --server=\$K8S_API_URL &&
                kubectl config set-credentials ci --token=\$K8S_CI_TOKEN &&
                kubectl config set-context ci --cluster=k8s --user=ci &&
                kubectl config use-context ci &&
                helm init --client-only &&
                helm upgrade --install ${helmRelease} .helm \
                ${helmDebug} \
                --namespace=${k8sNameSpace} \
                --set ingress.hostnamePrefix=${k8sIngressPrefix} \
                --set backend.image=${env.CI_REGISTRY_IMAGE} \
                --set backend.imageTag=${BR_NAME}-${env.BUILD_ID} \
                --set frontend.image=\$NGX_IMAGE \
                --set frontend.imageTag=${BR_NAME}-${env.BUILD_ID}-static \
                --wait \
                --timeout 180 ||
                (helm history --max 2 ${helmRelease} | head -n 2 | tail -n 1 | cut -f 1 | xargs helm rollback ${helmRelease} && exit 1)'
                """
            }
        }
}
