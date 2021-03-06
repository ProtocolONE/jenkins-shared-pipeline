def call() {
    echo "Start Deploy"

    script {
        withCredentials([string(credentialsId: 'K8S_CI_TOKEN', variable: 'K8S_CI_TOKEN')]) {
            
            k8sNameSpace="prod"
            k8sIngressPrefix=""
            helmRelease="${env.P1_PROJECT}-prod"            
            BR_NAME=URLDecoder.decode(env.BRANCH_NAME)
            BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
            helmDebug="--debug"

            sh "echo job_name: ${JOB_NAME}"

            sh "echo branch: ${BR_NAME} helm release: ${helmRelease}"

            def prodHostname = sh (returnStdout: true, script: "grep prodHostname .helm/values.yaml | awk '{print \$2}' | tr -d '\r\n'")

            registryImage = env.CI_REGISTRY_IMAGE

            if(env.BRANCH_NAME.startsWith("demo")){
                //prodHostname="api."+prodHostname
                k8sIngressPrefix="demo-"
                helmRelease=helmRelease+"-demo"
            }

            if(env.JOB_NAME.indexOf("qilin/auth1.protocol.one")!=-1){
                //k8sIngressPrefix="qilin-"+k8sIngressPrefix
                //helmRelease="qilin-"+helmRelease
                registryImage=registryImage+"-qilin"
            }

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
                p1hub/kubernetes-helm:latest \
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
                --set ingress.hostname=${prodHostname} \
                --set backend.image=${registryImage} \
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
