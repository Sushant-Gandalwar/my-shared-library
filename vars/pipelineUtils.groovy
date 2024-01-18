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
                            echo "Initialization code has an error for ${APP_Name}"
                        }
                    }
                }
            }
            stage('BUILD'){
                when{
                    expression{
                        parameters.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot'
                    }
                }
                steps{
                    script{
                        log.info("Running Reload, clean and compile")
                    }
                    sh '''
                       java version 
                    '''

                    sh "sbt reload"
                    sh "sbt clean"
                    sh "sbt compile"


                }
                post {
                    failure {
                        script {
                            echo "Initialization code has an error for ${APP_Name}"
                        }
                    }
                }
            }
        }
    }
}
