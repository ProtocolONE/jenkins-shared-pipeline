def call() {
    echo "Start building image"


    
        script {
                    if(params.PROD_RELEASE){
                        sh ''' echo "production release"'''              
                    } else {
                        sh ''' echo "test release"'''
                    }
                    checkout scm
                    BR_NAME=env.BRANCH_NAME
                    BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()

                    JENKINS_UID=sh(script: 'id -u', , returnStdout: true).trim()
                    JENKINS_GID=sh(script: 'id -g', , returnStdout: true).trim()

                    sh """
                        REGISTRY_IMAGE=$CI_REGISTRY_IMAGE
                        if(${JOB_NAME}.indexOf("qilin/auth1.protocol.one")!=-1){
                            REGISTRY_IMAGE="qilin-"+${CI_REGISTRY_IMAGE}
                        }

                        if [[ -f Makefile && ! -f Dockerfile ]]
                        then
                            GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make build-jenkins
                            GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make docker-image-jenkins
                        else
                            echo "CI_REGISTRY_IMAGE: $CI_REGISTRY_IMAGE"
                            docker build -t $REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID .
                            (if [ -f Dockerfile.nginx ]; then docker build -t $REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID-static -f Dockerfile.nginx . ; else echo "Project without static content"; fi);
                        fi
                    
                    """
                }
    echo "Pushing image"
        script {
                    BR_NAME=env.BRANCH_NAME
                    BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'p1docker',
                    usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
                    sh """
                        REGISTRY_IMAGE=$CI_REGISTRY_IMAGE
                        if(${JOB_NAME}.indexOf("qilin/auth1.protocol.one")!=-1){
                            REGISTRY_IMAGE="qilin-"+${CI_REGISTRY_IMAGE}
                        }
                        echo "CI_REGISTRY_IMAGE: $CI_REGISTRY_IMAGE"
                        docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
                        docker push $REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID
                        (if [ -f Dockerfile.nginx ]; then docker push $REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID-static ; else echo "Project without static content"; fi);
                    """
                    }
                }
}
