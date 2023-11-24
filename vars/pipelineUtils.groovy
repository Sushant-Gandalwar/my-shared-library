pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: scmUrl]])
                sh 'echo "Building the project"'
            }
        }

        stage('build image') {
            steps {
                script {
                    sh "docker build -t ${dockerImage} ."
                }
            }
        }

        stage('access image locally') {
            steps {
                script {
                    sh "docker run -p 8085:80 ${dockerImage}"
                }
            }
        }
    }
}
