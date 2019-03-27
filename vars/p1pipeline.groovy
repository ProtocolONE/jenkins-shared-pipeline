def call(project, registry, devBranch = "", devNameSpace = "",ingressPrefix="dev-") {

  pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
        }

    environment {
        CI_REGISTRY_IMAGE = "p1hub/${registry}"
        P1_PROJECT = "${project}"
    }

    agent any

    parameters {
        booleanParam(name: 'PROD_RELEASE', defaultValue: false, description: 'Release to production')
        booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Rollback project?')
    }    
    stages {
        if(params.ROLLBACK){
            stage('Rollback') {
                steps {
                    script {
                        p1rollback()
                    }
                }
            }
        } else {
            stage('Build') {
                steps {
                    script {
                        p1build()
                    }
                }
            }
    
            stage('SecScan') {
                steps {
                    script {
                        p1secscan()
                    }
                }
            }

            stage('Staging Deployment') {
                steps {
                    script {
                        p1deploy(devBranch, devNameSpace, ingressPrefix)
                    }
                }
            }
        }
    }

    post {
        success {
            slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        
        failure {
            slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
  }
}
