def call(project, registry, devBranch = "", devNameSpace = "",ingressPrefix="dev-") {

  pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
        }

    environment {
        CI_REGISTRY_IMAGE = "p1hub/${registry}"
        P1_PROJECT = "${project}"
        BRANCH_TO_BUILD_DEFAULT = 'develop'
        BRANCH_TO_BUILD_REQUESTED = "${params.BRANCH_TO_BUILD}"
    }

    agent any

    parameters {
        booleanParam(name: 'PROD_RELEASE', defaultValue: false, description: 'Release to production')
        booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Rollback project?')
        booleanParam(name: 'CUSTOM_BUILD', defaultValue: false, description: 'Want to choose a custom branch?')
        string(name: 'BRANCH_TO_BUILD', defaultValue: "develop", description: 'GIT branch to build')
    }    
    stages {
            stage('Rollback') {
                when {
                    expression {params.ROLLBACK == true}
                }
                steps {
                    script {
                        p1rollback()
                    }
                }
            }

            stage('CUSTOM_BUILD') {
                when {
                    expression {params.CUSTOM_BUILD == true}
                }
                steps {
                    script {
                        p1custombranch()
                    }
                }
            }

            stage('Build') {
                when {
                    expression {params.ROLLBACK == false}
                }
                steps {
                    script {
                        p1build()
                    }
                }
            }
    
            stage('SecScan') {
                when {
                    expression {params.ROLLBACK == false}
                }
                steps {
                    script {
                        p1secscan()
                    }
                }
            }

            stage('Staging Deployment') {
                when {
                    expression {params.ROLLBACK == false}
                }
                steps {
                    script {
                        p1deploy(devBranch, devNameSpace, ingressPrefix)
                    }
                }
            }
    }

    post {
        success {
            slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} branch: ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        
        failure {
            slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} branch: ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
  }
}
