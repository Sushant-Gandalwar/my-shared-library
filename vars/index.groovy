def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${pipelineParams.dockerImageTag}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'k8s-cluster'
            LOCATION = 'us-central1-c'
            SERVICE_ACCOUNT_KEY_CONTENT = credentials("${CREDENTIALS_ID}")
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        echo "Git URL: ${scmUrl}"
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
                        withDockerRegistry([credentialsId: "gcr:${CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${DOCKERDIRECTORY} && docker build -t ${IMAGE}:${IMAGE_TAG} -f Dockerfile ."
                            sh "docker push ${IMAGE}:${IMAGE_TAG}"
                        }
                    }
                }
            }

            stage('Deploy to GKE') {
                steps {
                    script {
                        // Create a temporary file for the service account key
                        def serviceAccountKeyFile = File.createTempFile("gcp-key", ".json")
                        serviceAccountKeyFile.write(SERVICE_ACCOUNT_KEY_CONTENT)

                        // Activate service account and get cluster credentials
                        sh "gcloud auth activate-service-account --key-file=${serviceAccountKeyFile}"
                        sh "gcloud container clusters get-credentials ${CLUSTER_NAME} --zone ${LOCATION}"

                        // Clean up the temporary file
                        serviceAccountKeyFile.delete()
                    }
                }
            }
        }
    }
}
