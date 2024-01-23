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
            GCP_PROJECT_ID = 'jenkins-407204'
            GKE_CLUSTER_NAME = 'sushant'
            GKE_CLUSTER_ZONE = 'us-central1'
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
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                             sh """
                                docker push '${env.IMAGE}:${env.IMAGETAG}'
                               
                                
                                """
                        }
                    }
                }
            }

            stage('Create GKE Cluster') {
                steps {
                    script {
                        // Authenticate with GCP using service account credentials
                        withCredentials([gcpServiceAccount(credentialsId:"gcr: ${env.CREDENTIALS_ID}", project: "id : ${GCP_PROJECT_ID}")]) {
                            // Set the GCP project and zone
                            sh "gcloud config set project ${GCP_PROJECT_ID}"
                            sh "gcloud config set compute/zone ${GKE_CLUSTER_ZONE}"

                            // Create GKE cluster
                            sh "gcloud container clusters create ${GKE_CLUSTER_NAME} --num-nodes=3 --machine-type=n1-standard-1"
                        }
                    }
                }
            }

            stage('ARC-DEV APPROVAL') {
                steps {
                    script {
                        sh "cd ${env.DOCKERDIRECTORY}"
                        sh "kubectl apply -f demo.yaml"
                        echo "done"
                    }
                }
            }
        }
    }
}
