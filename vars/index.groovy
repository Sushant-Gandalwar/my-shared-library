def call(Map pipelineParams) {
    pipeline {
        agent any

        parameters {
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release')
        }

        environment {
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


                       
                    }
                }
            }
        }
    }
}
