def call(Map pipelineParams) {
    pipeline {
        agent any

        parameters {
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release')
        }

        environment {
           APP_NAME = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            DB_CREDS_ID = "${pipelineParams.databaseCredentialsId}"
            IMAGE_TAG = "${params.Parameter}"
            COMMAND = "${params.Parameter}"
	    BRANCH =  "${pipelineParams.branch}"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo 'Start Initializing!'


                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl

                        // Use Jenkins build number as part of the Docker image tag
                        if (env.IMAGE_TAG == 'default' && pipelineParams.branch == 'main') {
                            env.IMAGETAG = "-${env.BUILD_NUMBER}"
                        } else {
                            env.IMAGETAG = env.IMAGE_TAG
                        }

                        echo "Image tag: ${env.IMAGE}:${env.IMAGETAG}"
                        echo "Build Number: ${env.BUILD_NUMBER}"
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
        }
    }
}
