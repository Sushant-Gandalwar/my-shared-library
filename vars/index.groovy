pipeline {
    agent any

    environment {
        PROJECT_ID = 'jenkins-407204'
        CLUSTER_NAME = 'k8s-cluster'
        LOCATION = 'us-central1-c'
        CREDENTIALS_ID = 'f3d27808a72f4b4584aa7f7edd4447d1'
        DOCKER_IMAGE_NAME = 'sushantgandalwar/hello'
    }

    stages {
        stage('INITIALIZE') {
            steps {
                script {
                    echo "Initializing environment for webstore delivery pipeline"
                    echo "Git URL: ${env.scmUrl}"
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

        stage("Build image") {
            steps {
                script {
                    def myapp = docker.build("${env.DOCKER_IMAGE_NAME}:${env.BUILD_ID}")
                }
            }
        }

        stage("Push image") {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub') {
                        docker.image("${env.DOCKER_IMAGE_NAME}:${env.BUILD_ID}").push()
                        docker.image("${env.DOCKER_IMAGE_NAME}:latest").push()
                    }
                }
            }
        }

        stage('Deploy to GKE') {
            steps {
                script {
                    sh "sed -i 's|${env.DOCKER_IMAGE_NAME}:latest|${env.DOCKER_IMAGE_NAME}:${env.BUILD_ID}|g' deployment.yaml"
                    step([$class: 'KubernetesEngineBuilder', projectId: env.PROJECT_ID, clusterName: env.CLUSTER_NAME, location: env.LOCATION, manifestPattern: 'deployment.yaml', credentialsId: env.CREDENTIALS_ID, verifyDeployments: true])
                }
            }
        }
    }
}
