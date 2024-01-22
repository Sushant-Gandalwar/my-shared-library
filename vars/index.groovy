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
                            log.error("Initialization code has an error for ${APP_Name}")
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
                         docker push jenkins-407204/'${env.IMAGE}:${env.IMAGETAG}'
                        
                         
                         """
                    }
                    }
                    
                }
            }
            stage('CONTAINER') {
                                 
                steps {
                    script {
                        sh "docker run -p 8086:3000 ${env.IMAGE}:${env.IMAGETAG}"
                        
                    }

                    
                }    
            }
        }
    }
}
