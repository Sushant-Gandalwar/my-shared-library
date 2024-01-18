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
		   // sh "sbt dependencyTree"
                }
                post {
                  failure {
                    script {
                      echo "Build and compile failed for Service: ${APP_NAME}"
		         }
                  }
                }
            }
        }
    }
}
