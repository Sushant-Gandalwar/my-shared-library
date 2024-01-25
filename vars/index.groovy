def call(Map pipelineParams) {
    pipeline {
        agent any

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

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGE_TAG}' -f Dockerfile ."
                            sh "docker push '${env.IMAGE}:${env.IMAGE_TAG}'"
                        }
                    }
                }
            }

            stage('Pull Docker Image from Google Container Registry') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "docker pull gcr.io/jenkins-407204/demo@sha256:c2eb93ab79ef155ee920c2c519451d69e2bd7362d67c5069a278f93bbce1493f"
                        }
                    }
                }
            }
            stage('Pull Docker Image from Google Container Registry') {
                steps {
                    script {
                        sh "kubectl apply -f /var/lib/jenkins/workspace/demo/deployment.yaml"
                    }
                }
            }
        }
    }
}
