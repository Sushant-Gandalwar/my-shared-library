def call(Map pipelineParams) {
    pipeline {
        agent any
        parameters {
            choice(
                name: 'Build_Type',
                choices: 'BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release',
                description: 'Select the Build type'
            )
            string(
                name: 'Parameter',
                defaultValue: 'default',
                description: 'Pass the Docker image id if choosing DEPLOY_ONLY OR pass the sbt release command if choosing Publish_to_Release'
            )
        }

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            CREDENTIALS_PASS = "${pipelineParams.dockerCredentialsPass}"
            // DB_CREDS_ID = "${pipelineParams.databaseCredentialsId}"
            IMAGE_TAG = "${params.Parameter}"
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
            stage('BUILD') {
            when {
                expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
            }            
                steps {
                    script {
                        echo "Running Reload, clean and compile"
                    }
		     sh ''' 
		          java -version 
		       	'''
                    sh "sbt reload && sbt clean && sbt compile"
		  
                }
                post {
                  failure {
                    script {
                      echo "Build and compile failed for Service: ${APP_NAME}"
		         }
                  }
                }
            }
            stage('BUILD IMAGE') {
    when {
        expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
    }
    steps {
        script {
            echo "Building Docker image"
        }

        // Ensure that sbt is installed
        sh 'sbt clean compile'

        // Publish the Docker image locally using sbt
        sh 'sbt docker:publishLocal'

        withDockerRegistry([credentialsId: "dockerhub-credentials", url: "https://index.docker.io/v1/"]) {
    // Change to the Docker directory
    dir("${env.DOCKERDIRECTORY}") {
        // Build the Docker image
        sh "docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."

        // Log in to Docker Hub (using credentials)
        sh "docker login -u ${env.credentialsId} -p ${env.credentialsPass}"

        // Push the Docker image to Docker Hub
        sh "docker push '${env.IMAGE}:${env.IMAGETAG}'"

        // Log out from Docker Hub
        sh "docker logout"

        // Remove the local Docker image (optional)
        sh "docker rmi '${env.IMAGE}:${env.IMAGETAG}'"
    }
}

    }
    post {
        failure {
            script {
                echo "Failed to build Docker image."
            }
        }
    }
}

        }
    }
}
