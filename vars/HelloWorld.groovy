// MySharedLibrary/vars/DockerPipeline.groovy

def call(String gitUrl, String gitBranch, String imageName) {
    pipeline {
        agent any

        stages {
            stage('Checkout') {
                steps {
                    checkout scmGit(branches: [[name: gitBranch]], extensions: [], userRemoteConfigs: [[url: gitUrl]])
                    sh 'echo "Building the project"'
                }
            }

            stage('Build Image') {
                steps {
                    script {
                        sh "docker build -t ${imageName} ."
                    }
                }
            }

            stage('Access Image Locally') {
                steps {
                    script {
                        sh "docker run -p 8085:3000 ${imageName}"
                    }
                }
            }
        }
    }
}
