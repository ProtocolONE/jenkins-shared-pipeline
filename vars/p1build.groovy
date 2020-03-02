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

                    registryImage=env.CI_REGISTRY_IMAGE
                    if(env.JOB_NAME.indexOf("qilin/auth1.protocol.one")!=-1){
                        registryImage=env.CI_REGISTRY_IMAGE+"-qilin"
                    }


                    sh """
                        if [[ -f Makefile && ! -f Dockerfile ]]
                        then
                            GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make build-jenkins
                            GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make docker-image-jenkins
                        else
                            echo "REGISTRY_IMAGE: ${registryImage}"
                            docker build -t ${registryImage}:${BR_NAME}-$BUILD_ID -t ${registryImage}:${BR_NAME} .
                            (if [ -f Dockerfile.nginx ]; then docker build -t ${registryImage}:${BR_NAME}-$BUILD_ID-static -f Dockerfile.nginx . ; else echo "Project without static content"; fi);
                        fi
                    
                    """
                }
    echo "Pushing image"
        script {
                    BR_NAME=env.BRANCH_NAME
                    BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'p1docker',
                        usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) 
                    {
                        registryImage = env.CI_REGISTRY_IMAGE
                        if(env.JOB_NAME.indexOf("qilin/auth1.protocol.one")!=-1){
                            registryImage=env.CI_REGISTRY_IMAGE+"-qilin"
                        }
                        sh """
                            echo "CI_REGISTRY_IMAGE: ${registryImage}"
                            docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
                            docker push ${registryImage}:${BR_NAME}-$BUILD_ID
                            if [[ ! -f Makefile ]]
                            then
                                docker push ${registryImage}:${BR_NAME}
                            fi
                            (if [ -f Dockerfile.nginx ]; then docker push ${registryImage}:${BR_NAME}-$BUILD_ID-static ; else echo "Project without static content"; fi);
                        """
                    }
                }
}
