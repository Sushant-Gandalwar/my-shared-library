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
            IMAGE_TAG = "${params.Parameter}"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        def log =  new LogWrapper(currentBuild.rawBuild.logger)  // Initialize log
                        log.info("Initializing environment for webstore delivery pipeline")
                        log.info("Git URL: ${env.scmUrl}")
                    }
                }
                post {
                    failure {
                        script {
                            def log =  new LogWrapper(currentBuild.rawBuild.logger)  // Initialize log
                            log.error("Initialization code has an error for ${APP_Name}")
                        }
                    }
                }
            }

            stage('PUBLISH IMAGE') {
                when {
                    expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
                }
                steps {
                    script {
                        def log =  new LogWrapper(currentBuild.rawBuild.logger)  // Initialize log
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
                        def log =  new LogWrapper(currentBuild.rawBuild.logger)  // Initialize log
                        log.info("Published Docker image ${env.IMAGE} to GCR")
                    }
                }
            }
        }
    }
}
