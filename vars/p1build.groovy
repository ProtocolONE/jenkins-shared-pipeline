def call() {
    echo "Start building image"

        script {
                    if(params.PROD_RELEASE){
                        sh ''' echo "production release"'''              
                    } else {
                        sh ''' echo "test release"'''
                    }
                    checkout scm
                        sh '''
                        docker build -t $CI_REGISTRY_IMAGE:$BUILD_ID .
                        (if [ -f Dockerfile.nginx ]; then docker build -t $CI_REGISTRY_IMAGE:"$BUILD_ID"-static -f Dockerfile.nginx . ; else echo "Project without static content"; fi);
                        '''
                }
    echo "Pushing image"
        script {
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'p1docker',
                    usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD']]) {
                    sh '''
                        docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
                        docker push $CI_REGISTRY_IMAGE:$BUILD_ID
                        (if [ -f Dockerfile.nginx ]; then docker push $CI_REGISTRY_IMAGE:"$BUILD_ID"-static ; else echo "Project without static content"; fi);
                    '''
                    }
                }
}
