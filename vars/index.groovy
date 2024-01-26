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
	    GKE_SA_KEY_CREDENTIALS = credentials(f3d27808a72f4b4584aa7f7edd4447d1)
	    PROJECT_ID = 'jenkins-407204'
        CLUSTER_NAME = 'k8s-cluster'
        LOCATION =  'us-central1-c'
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
            stage('Authenticate with GCP') {
                steps {
                    script {
                            withCredentials([file(credentialsId: 'gke-service-account-key', variable: 'GKE_SA_KEY_CREDENTIALS')]) {
                        // Your script here, accessing $GKE_SA_KEY_CREDENTIALS
                        sh "gcloud auth activate-service-account --key-file=${GKE_SA_KEY_CREDENTIALS}"
                        sh "gcloud container clusters get-credentials <CLUSTER_NAME> --region <REGION>"
                        // Other deployment steps
                        }
                    }
                }
            }
        }
    }
}


