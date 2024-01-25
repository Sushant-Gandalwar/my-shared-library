def call(Map pipelineParams) {
    pipeline {
        agent any
        tools {
            maven 'Maven'
        }
        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            IMAGE_TAG = "${params.Parameter}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            PROJECT_ID = 'jenkins-407204'
                CLUSTER_NAME = 'k8s-cluster'
                LOCATION = 'us-central1-c'
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
	    
	    stage('Build') {
		    steps {
			    sh 'mvn clean package'
		    }
	    }
	    
	    stage('Test') {
		    steps {
			    echo "Testing..."
			    sh 'mvn test'
		    }
	    }

            stage('Build Docker Image') {
                steps {
                    script {
                        sh 'whoami'
                        myimage = docker.build("sushantgandalwar/devops:${env.BUILD_ID}")
                    }
                }
            }

            stage("Push Docker Image") {
                steps {
                    script {
                        echo "Push Docker Image"
                        withCredentials([string(credentialsId: 'dockerhub', variable: 'dockerhub')]) {
                            sh "docker login -u sushantgandalwar -p ${dockerhub}"
                        }
                        myimage.push("${env.BUILD_ID}")
                    }
                }
            }
            stage('Deploy to K8s') {
		    steps{
			    echo "Deployment started ..."
			    sh 'ls -ltr'
			    sh 'pwd'
			    sh "sed -i 's/tagversion/${env.BUILD_ID}/g' serviceLB.yaml"
				sh "sed -i 's/tagversion/${env.BUILD_ID}/g' deployment.yaml"
			    echo "Start deployment of serviceLB.yaml"
			    step([$class: 'KubernetesEngineBuilder', projectId: env.PROJECT_ID, clusterName: env.CLUSTER_NAME, location: env.LOCATION, manifestPattern: 'serviceLB.yaml', credentialsId: env.CREDENTIALS_ID, verifyDeployments: true])
				echo "Start deployment of deployment.yaml"
				step([$class: 'KubernetesEngineBuilder', projectId: env.PROJECT_ID, clusterName: env.CLUSTER_NAME, location: env.LOCATION, manifestPattern: 'deployment.yaml', credentialsId: env.CREDENTIALS_ID, verifyDeployments: true])
			    echo "Deployment Finished ..."
		    }
	    }
        }
    }
}
