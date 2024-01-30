def call(Map pipelineParams) {
    pipeline {
        agent any
        environment {
            scmUrl = 'https://github.com/Sushant-Gandalwar/Jenkins-Docker-Kubernetes-Project3'
            APP_Name = "${pipelineParams.appName}"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        git branch: 'main', credentialsId: 'sushant-git', url: pipelineParams.scmUrl
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
