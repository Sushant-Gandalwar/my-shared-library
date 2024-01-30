def call(Map pipelineParams) {
    pipeline {
        agent any

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo "Initializing environment for webstore delivery pipeline"
                        git branch: 'main', credentialsId: 'sushant-git', url: 'https://github.com/Sushant-Gandalwar/Jenkins-Docker-Kubernetes-Project3.git'
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
