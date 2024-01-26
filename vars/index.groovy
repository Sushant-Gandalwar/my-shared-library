def call(Map pipelineParams) {
    pipeline {
        agent any

        parameters {
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release')
        }

        tools {
            // Specify the default Git tool installation
            defaultTool 'Default'
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
                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl

                        // Use Jenkins build number as part of the Docker image tag
                        if (env.IMAGE_TAG == 'default' && pipelineParams.branch == 'main') {
                            env.IMAGETAG = "-${env.BUILD_NUMBER}"
                        } else {
                            env.IMAGETAG = env.IMAGE_TAG
                        }

                        echo "Image tag: ${env.IMAGE}:${env.IMAGETAG}"
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

            stage('Build, Rename, and Push Docker Image') {
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
        }
    }
}
