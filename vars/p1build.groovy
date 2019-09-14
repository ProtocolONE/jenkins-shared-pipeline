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
                    BR_NAME=BR_NAME.replaceAll("/","-").toLowerCase()

                    JENKINS_UID=sh(script: 'id -u', , returnStdout: true).trim()
                    JENKINS_GID=sh(script: 'id -g', , returnStdout: true).trim()

                    sh """
                    if [ -f Makefile ]
                    then
                        DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make vendor
                        DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make build
                        DIND=1 TAG=${BR_NAME}-$BUILD_ID DIND_UID=$JENKINS_UID DIND_GUID=$JENKINS_GID make docker-image
                    else
                        docker build -t $CI_REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID .
                        (if [ -f Dockerfile.nginx ]; then docker build -t $CI_REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID-static -f Dockerfile.nginx . ; else echo "Project without static content"; fi);
                    fi
                    
                    """
                }
    echo "Pushing image"
        script {
                    BR_NAME=env.BRANCH_NAME
                    BR_NAME=BR_NAME.replaceAll("/","-").toLowerCase()
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'p1docker',
                    usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
                    sh """
                        docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
                        docker push $CI_REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID
                        (if [ -f Dockerfile.nginx ]; then docker push $CI_REGISTRY_IMAGE:${BR_NAME}-$BUILD_ID-static ; else echo "Project without static content"; fi);
                    """
                    }
                }
}
