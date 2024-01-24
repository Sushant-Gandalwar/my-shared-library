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
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'k8s-cluster'
            LOCATION = 'us-central1-c'
               
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
                            sh "docker push '${env.IMAGE}:${env.IMAGETAG}'"
                        }
                    }
                }
            }
	     stage('Deploy to K8s') {
		    steps{
			    echo "Deployment started ..."
			    sh "kubectl apply -f /var/lib/jenkins/workspace/java/pod.yaml"
		    }
	    }
        }
    }
}
