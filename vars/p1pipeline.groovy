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
        TAG_TO_BUILD_REQUESTED = "${params.TAG_TO_BUILD}"
    }

    agent any

    parameters {
        booleanParam(name: 'BUILD_WITHOUT_CACHE', defaultValue: false, description: 'Skip cached docker layers?')
        booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Rollback project?')
        booleanParam(name: 'CUSTOM_BUILD', defaultValue: false, description: 'Want to choose a custom branch?')
        string(name: 'BRANCH_TO_BUILD', defaultValue: "develop", description: 'GIT branch to build')
        booleanParam(name: 'STG_RELEASE', defaultValue: false, description: 'Release to stg? Please provide BRANCH_TO_BUILD or TAG_TO_BUILD')
        string(name: 'TAG_TO_BUILD', defaultValue: "v1.0", description: 'GIT tag to build')
        booleanParam(name: 'PROD_RELEASE', defaultValue: false, description: 'Release to production? Please provide TAG_TO_BUILD')
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

            stage('Tst Deployment') {
                when {
                    expression {params.ROLLBACK == false && params.PROD_RELEASE == false && params.STG_RELEASE == false}
                }
                steps {
                    script {
                        p1deploy(devBranch, devNameSpace, ingressPrefix)
                    }
                }
            }

            stage('Stg Deployment') {
                when {
                    expression {params.ROLLBACK == false && params.STG_RELEASE == true}
                }
                steps {
                    script {
                        p1deploystg()
                    }
                }
            }

            stage('Prod Deployment') {
                when {
                    expression {params.ROLLBACK == false && params.PROD_RELEASE == true}
                }
                steps {
                    script {
                        p1deployprod()
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
