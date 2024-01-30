def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
            CLUSTER_NAME = 'demo'
            LOCATION =  'us-central1'
            IMAGE_TAG = "${env.BUILD_NUMBER}"  
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[url: "${env.scmUrl}", credentialsId: "${pipelineParams.bitbucketCredentialsId}"]]])
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
