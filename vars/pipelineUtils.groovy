def call(Map pipelineParams) {
    pipeline {
        agent any
        parameters {
            choice(
                name: 'Build_Type',
                choices: 'BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release',
                description: 'Select the Build type'
            )
            string(
                name: 'Parameter',
                defaultValue: 'default',
                description: 'Pass the Docker image id if choosing DEPLOY_ONLY OR pass the sbt release command if choosing Publish_to_Release'
            )
        }

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
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
                            log.error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }
            // stage('BUILD'){
            //     when{
            //         expression{
            //             params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot'
            //         }
            //     }
            //     steps{
            //         script{
            //             log.info("Running Reload, clean and compile")
            //         }
            //         sh '''
            //            java version 
            //         '''

            //         sh "sbt reload"
            //         sh "sbt clean"
            //         sh "sbt compile"


            //     }
            //     post {
            //         failure {
            //             script {
            //                 log.error("Initialization code has an error for ${APP_Name}")
            //             }
            //         }
            //     }
            // }
             stage('PUBLISH IMAGE') {
               when {
                     expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
               }                        
                steps {
                    script {
                        log.info("Building docker image and publishing to GCR")
                    }
                    sh "sbt publish"
                    sh "sbt docker:publishLocal"
                    withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://hub.docker.com/orgs"]) {
                      sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                      sh """
                         docker push '${env.IMAGE}:${env.IMAGETAG}'
                         docker rmi '${env.IMAGE}:${env.IMAGETAG}'
                         
                         """
                    }


                    script {
                        log.info("Published Docker image ${env.IMAGE} to GCR")
                    }
                }
            }
        }
    }
}
