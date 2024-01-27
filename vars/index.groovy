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

            stage('Build, Rename, and Push Docker Image') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGE_TAG}' -f Dockerfile ."
                            sh "docker push '${env.IMAGE}:${env.IMAGE_TAG}'"
                        }
                    }
                }
            }
           stage('ARC-DEV APPROVAL') {
               
                steps {
                    script {
                        echo "Approval is required to perform deployment in DEV, Click 'Proceed or Abort'"
                    }

                    timeout(time: 2, unit: 'HOURS') {
                        verifybuild()
                    }
                }
            }

            stage('Deploy to K8s') {
                when {
                    expression { env.releaseskip == 'dorelease' }
                }
                steps {
                    echo "Deployment started ..."
                    sh 'ls -ltr'
                    sh 'pwd'
                    sh "sed -i 's/tagversion/${env.BUILD_NUMBER}/g' serviceLB.yaml"
                    sh "sed -i 's/tagversion/${env.BUILD_NUMBER}/g' deployment.yaml"
                    
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

def verifybuild() {

        def userInput = input(
            id: 'userInput', message: 'Approve Deployment!',        parameters: [

      [$class: 'BooleanParameterDefinition', defaultValue: 'false', description: 'click to skip', name: 'skip'],
    ])

        if(!userInput) {
            env.releaseskip = 'dorelease'
            }
            else {
                env.releaseskip = 'norelease'

            }

    }


