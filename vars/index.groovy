def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = 'hello-world-html'
            IMAGETAG = 'latest' // You can parameterize this based on your needs
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
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                        }
                    }
                }
            }

            stage('ARC-DEV APPROVAL') {
                when {
                    expression { true } // Always evaluate to true
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
                        sh "docker run -p 8085:8000 ${env.IMAGE}:${env.IMAGETAG}"
                    }
                }
            }
        }
    }
}

def verifybuild() {
    def userInput = input(
        id: 'userInput', message: 'Approve Deployment!', parameters: [
            [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'click to skip', name: 'skip'],
        ])

    if (!userInput) {
        env.releaseskip = 'dorelease'
    } else {
        env.releaseskip = 'norelease'
    }
}
