def call(Map pipelineParams) {
    pipeline {
        agent any

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.git}"
            sushant-git = "${pipelineParams.appName}"
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
                         checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[credentialsId: 'sushant-git', url: env.scmUrl]]])
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

            stage('PUBLISH IMAGE') {
                steps {
                    script {
                        withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                            sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGE_TAG}' -f Dockerfile ."
                            sh "docker push '${env.IMAGE}:${env.IMAGE_TAG}'"
                        }
                    }
                }
            }
           stage('ARC-QA APPROVAL') {
               
                steps {
                    script {
                        echo "Approval is required to perform deployment in DEV, Click 'Proceed or Abort'"
                    }

                    timeout(time: 2, unit: 'HOURS') {
                        verifybuild()
                    }
                }
            }

            stage('ARC-QA DEPLOY') {
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
                 post {
                    failure {
                        script {
                            error("Deployment failed has an error for ${CLUSTER_NAME}")
                        }
                    }
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


