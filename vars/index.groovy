def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE_TAG = "${params.Parameter}"
            IMAGE = "${pipelineParams.dockerImage}"
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
                        sh 'cd ${env.DOCKERDIRECTORY}'
                        sh "docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."

                        // Login to Docker Hub
                        // sh 'docker login -u sushant900123 -p Sush900123@'

                        // Tag the Docker image
                        // sh 'docker tag jaydeep sushant900123/hello-world-html:latest'

                        // Push the Docker image to Docker Hub
                        // sh 'docker push sushant900123/hello-world-html:latest'
                    }
                }
            }
        }


    }
}
