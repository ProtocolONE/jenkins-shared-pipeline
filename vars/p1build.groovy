def call() {
    echo "Start building image" 
        script {
                    if(params.PROD_RELEASE){
                        sh ''' echo "production release"'''              
                        env.BRANCH_NAME=TAG_TO_BUILD_REQUESTED

                        checkout scm: [
                            $class: 'GitSCM',
                            branches: [[name: "refs/tags/${BRANCH_NAME}"]],
                            userRemoteConfigs: [
                                [url: env.GIT_URL,
                                refspec: "+refs/tags/${BRANCH_NAME}",
                                credentialsId: 'p1release']
                            ]
                        ]                        
                    } else {
                        sh ''' echo "test release"''' 
                        checkout scm: [
                            $class: 'GitSCM',
                            branches: [[name: env.BRANCH_NAME]],
                            userRemoteConfigs: [
                                [url: env.GIT_URL,
                                refspec: "+refs/heads/${BRANCH_NAME}:refs/remotes/origin/${BRANCH_NAME}",
                                credentialsId: 'p1release']
                            ]
                        ]
                    }
                    
                    BR_NAME=env.BRANCH_NAME
                    BR_NAME=BR_NAME.replaceAll("/","-").replaceAll("_","-").replaceAll("#","").toLowerCase()

                    JENKINS_UID=sh(script: 'id -u', , returnStdout: true).trim()
                    JENKINS_GID=sh(script: 'id -g', , returnStdout: true).trim()

                    registryImage=env.CI_REGISTRY_IMAGE
                    if(env.JOB_NAME.indexOf("qilin/auth1.protocol.one")!=-1){
                        registryImage=env.CI_REGISTRY_IMAGE+"-qilin"
                    }

                    if(params.BUILD_WITHOUT_CACHE){
                        skipcache = "--no-cache"
                    } else {
                        skipcache = ""
                    }

                    withCredentials([string(credentialsId: 'GITHUB_TOKEN', variable: 'TOKEN')]) {

                        sh """
                            if [[ -f Makefile && ! -f Dockerfile ]]
                            then
                                GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make build-jenkins
                                GOPATH=/go DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make docker-image-jenkins
                                GOPATH=/go DIND=1 TAG=${BR_NAME} DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make docker-image-jenkins
                            else
                                echo "REGISTRY_IMAGE: ${registryImage}"
                                docker build ${skipcache} --build-arg TOKEN -t ${registryImage}:${BR_NAME}-$BUILD_ID -t ${registryImage}:${BR_NAME} .
                                (if [ -f Dockerfile.nginx ]; then docker build -t ${registryImage}:${BR_NAME}-$BUILD_ID-static -t ${registryImage}:${BR_NAME}-static -f Dockerfile.nginx . ; else echo "Project without static content"; fi);
                            fi
                        
                        """
                    }
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
                            #if [[ ! -f Makefile ]]
                            #then
                                docker push ${registryImage}:${BR_NAME}
                            #fi
                            (if [ -f Dockerfile.nginx ]; then docker push ${registryImage}:${BR_NAME}-$BUILD_ID-static && docker push ${registryImage}:${BR_NAME}-static; else echo "Project without static content"; fi);
                        """
                    }
                }
}
