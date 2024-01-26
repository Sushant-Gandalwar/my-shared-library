def call(Map pipelineParams) {
    pipeline {
        agent any

        parameters {
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release')
        }

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${params.Parameter}"
            COMMAND = "${params.Parameter}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'demo'
            LOCATION =  'us-central1'
            BRANCH =  "${pipelineParams.branch}"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo 'Start Initializing!'

                        // Specify the Git tool
                        def gitTool = tool 'Default'

                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl, gitTool: gitTool

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
