def call() {
    stages {
        stage('Build') {
            steps {
                script {
                    build()
                }
            }
        }

        stage('Run Tests') {
            parallel {
                stage('Test 1') {
                    steps {
                        sh "sleep 5"
                    }
                }
                stage('Test 2') {
                    steps {
                        sh "sleep 5"
                    }
                }
            }
        }

        stage('Staging Deployment') {
            steps {
                script {
                    deploy()
                }
            }
        }
    }

}
