def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            DOCKER_HUB_USERNAME = 'sushant900123'
            DOCKER_IMAGE_NAME = 'hello-world-html'
            DOCKER_IMAGE_TAG = 'latest' // You can parameterize this based on your needs
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
                            log.error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    script {
                        dir(env.DOCKERDIRECTORY) {
                            // Build the Docker image
                            sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -f Dockerfile ."

                            // Login to Docker Hub and push the image
                            withCredentials([usernamePassword(credentialsId: env.CREDENTIALS_ID, usernameVariable: 'DOCKER_HUB_USERNAME', passwordVariable: 'DOCKER_HUB_PASSWORD')]) {
                                sh "echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USERNAME} --password-stdin"
                                sh "docker push ${DOCKER_HUB_USERNAME}/${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                            }
                        }
                    }
                }
            }
            stage('ARC-DEV APPROVAL') {
                 when {
                    expression { pipelineParams.branch != 'development' }
                }                  
                steps {
                    script {
                        echo "Approval is required to perform deployment in DEV, Click 'Proceed or Abort'"
                    }

                    timeout(time: 2, unit: 'HOURS') {
		      verifybuild()
                    }
                }               
            }
             stage('CONTAINER') {
                                 
                steps {
                    script {
                        sh "docker run -p 8086:3000 ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                        
                    }

                    
                }               
            }
        }
    }
}


def verifybuild() {

        def userInput = input(
            id: 'userInput', message: 'Approve Deployment!',parameters: [

      [$class: 'BooleanParameterDefinition', defaultValue: 'false', description: 'click to skip', name: 'skip'],
    ])

        if(!userInput) {
            env.releaseskip = 'dorelease'
            }
            else {
                env.releaseskip = 'norelease'

            }

    }
