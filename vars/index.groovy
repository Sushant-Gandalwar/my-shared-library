def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = 'hello-world-html'
            IMAGETAG = 'latest' // You can parameterize this based on your needs
            CREDENTIALS_ID = '404b3183-6431-48ad-b984-2316e79f2cfd'
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
                        // Build the Docker image
            sh "cd ${env.DOCKERDIRECTORY} && docker build -t ${env.IMAGE}:${env.IMAGETAG} -f Dockerfile ."

            // Log in to the Docker registry
            sh "docker login -u _token -p 404b3183-6431-48ad-b984-2316e79f2cfd https://gcr.io"

            // Push the Docker image to the registry
            sh "docker push ${env.IMAGE}:${env.IMAGETAG}"
                    }
                    
                }
            }
        }
    }
}
