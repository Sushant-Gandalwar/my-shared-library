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
            stage('PUBLISH IMAGE') {
    when {
        expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
    }
    steps {
        script {
            echo "Building docker image and publishing to GCR"
        }

        // Ensure that sbt and Docker are installed
        sh 'sbt clean compile'

        // Publish the Docker image locally using sbt
        sh 'sbt docker:publishLocal'

        // Build and push the Docker image to GCR
        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
            sh "docker push '${env.IMAGE}:${env.IMAGETAG}'"
            sh "docker rmi '${env.IMAGE}:${env.IMAGETAG}'"
        }

        script {
            echo "Published Docker image ${env.IMAGE}:${env.IMAGETAG} to GCR"
        }
    }
    post {
        failure {
            script {
                echo "Failed to build and publish Docker image."
            }
        }
    }
}

        }
    }
}
