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
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                             sh """
                                docker push '${env.IMAGE}:${env.IMAGETAG}'
                               
                                
                                """
                        }
                    }
                }
            }
           stage('ARC-DEV APPROVAL') {
               
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
                when {
                    expression { env.releaseskip == 'dorelease' }
                }
                steps {
                    script {
                        sh "docker run -p 8083:3000 ${env.IMAGE}:${env.IMAGETAG}"
                    }
                }
            }
        }
    }
}

def verifybuild() {

        def userInput = input(
            id: 'userInput', message: 'Approve Deployment!',        parameters: [

      [$class: 'BooleanParameterDefinition', defaultValue: 'false', description: 'click to skip', name: 'skip'],
    ])

        if(!userInput) {
            env.releaseskip = 'dorelease'
            }
            else {
                env.releaseskip = 'norelease'

            }

    }
