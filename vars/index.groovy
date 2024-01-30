pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
               git branch: 'main', credentialsId: 'sushant-git', url: 'https://github.com/Sushant-Gandalwar/Jenkins-Docker-Kubernetes-Project3.git'
            }
        }
    }
}
