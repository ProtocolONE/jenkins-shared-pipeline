def call() {

    script {
            helmRelease = env.P1_PROJECT
            withCredentials([string(credentialsId: 'K8S_CI_TOKEN', variable: 'K8S_CI_TOKEN')]) {
                def Rev = sh (
                    returnStdout: true,
                    script: "docker run --rm p1hub/kubernetes-helm:2.11.0 /bin/sh -c 'kubectl config set-cluster k8s --insecure-skip-tls-verify=true --server=${K8S_API_URL} && kubectl config set-credentials ci --token=${K8S_CI_TOKEN} &&	kubectl config set-context ci --cluster=k8s --user=ci && kubectl config use-context ci && helm init --client-only && helm history --max 5 ${helmRelease} | head -6 | tail -n 5 | cut -f 1,2' | tail -n 5" 
                )

                def selectRev = input message: 'Please Provide Parameters', ok: 'Next',
                    parameters: [
                        [
                        $class: 'ChoiceParameterDefinition',
                        name: 'REVISION', 
                        choices: Rev,
                        description: 'Helm revisions'
                        ],
                    ]
                
                def selectedRev = sh (returnStdout: true, script: "echo ${selectRev} | cut -d ' ' -f 1")
                
                echo "You selected revision number: ${selectedRev}"
                
                def rollback = sh (
                    returnStdout: true,
                    script: "docker run --rm p1hub/kubernetes-helm:2.11.0 /bin/sh -c 'kubectl config set-cluster k8s --insecure-skip-tls-verify=true --server=${K8S_API_URL} && kubectl config set-credentals ci --token=${K8S_CI_TOKEN} &&	kubectl config set-context ci --cluster=k8s --user=ci && kubectl config use-context ci && helm init --client-only && helm rollback ${helmRelease} ${selectedRev}" 
                )
            }
    }
}
