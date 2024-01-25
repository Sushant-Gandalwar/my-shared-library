def call(Map pipelineParams) {
    pipeline {
        agent any
        tools {
            maven 'Maven'
        }
        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${params.Parameter}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        echo "Git URL: ${env.scmUrl}"
                    }
                }
                post {
                    failure {
                        script {
                            error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        sh 'whoami'
                        myimage = docker.build("sushantgandalwar/devops:${env.BUILD_ID}")
                    }
                }
            }

            stage("Push Docker Image") {
                steps {
                    script {
                        echo "Push Docker Image"
                        withCredentials([string(credentialsId: 'dockerhub', variable: 'dockerhub')]) {
                            sh "docker login -u sushantgandalwar -p ${dockerhub}"
                        }
                        myimage.push("${env.BUILD_ID}")
                    }
                }
            }
        }
    }
}
