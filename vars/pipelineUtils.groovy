class pipelineUtils {
    def call(Map pipelineParams) {
        pipeline {
            agent any
            parameters {
                choice(name: 'Build_Type', choices: 'BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release', description: 'Select the Build type')
                string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if chosen DEPLOY_ONLY OR pass the sbt release command if chosen Publish_to_Release')
            }
            environment {
                APP_NAME = "${pipelineParams.appName}"
                DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
                IMAGE = "${pipelineParams.dockerImage}"
                IMAGE_TAG = "${params.Parameter}"
                BRANCH = "${pipelineParams.branch}"
            }
            stages {
                stage('INITIALIZE') {
                    steps {
                        script {
                            echo "Initializing environment for webstore delivery pipeline"
                            git scmGit(pipelineParams.scmUrl, pipelineParams.branch)
                            if (env.IMAGE_TAG == 'default' && pipelineParams.branch == 'master') {
                                env.PACKAGE_VERSION = sh(script: "cat version.sbt | cut -d '=' -f2 | tr -d '\"'", returnStdout: true).trim()
                                env.IMAGETAG = 'v' + env.PACKAGE_VERSION + '-' + env.BUILD_NUMBER
                            } else if (env.IMAGE_TAG != 'default' && pipelineParams.branch == 'master') {
                                env.IMAGETAG = env.IMAGE_TAG
                            } else if (env.IMAGE_TAG == 'default') {
                                env.GIT_COMMIT_ID = sh(script: "git log -1 --pretty=%h", returnStdout: true).trim()
                                env.IMAGETAG = 'v' + env.GIT_COMMIT_ID + '-' + env.BUILD_NUMBER
                            } else {
                                env.IMAGETAG = env.IMAGE_TAG
                            }
                        }
                    }
                }
 
                stage('BUILD') {
                    when {
                        expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
                    }
                    steps {
                        script {
                            echo "Running Reload, clean and compile"
                            sh 'java -version'
                            sh "sbt reload && sbt clean && sbt compile"
                        }
                    }
                    post {
                        failure {
                            script {
                                echo "Build and compile failed for Service: ${APP_NAME}"
                            }
                        }
                    }
                }
 
                stage('TEST') {
                    when {
                        expression { pipelineParams.unitTests == true }
                        expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
                    }
                    steps {
                        script {
                            echo "Executing unit tests"
                            sh "sbt test"
                        }
                    }
                    post {
                        failure {
                            script {
                                echo "Unit tests failed for Service: ${APP_NAME}"
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
                            echo "Building docker image and publishing to GCR"
                            sh "sbt publish"
                            sh "sbt docker:publishLocal"
                            withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                                sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                                sh "docker push '${env.IMAGE}:${env.IMAGETAG}'"
                                sh "docker run -v $PWD:/workspace -w /workspace alpine rm -rf ${env.DOCKERDIRECTORY}/target"
                                sh "docker run -p 8085:80 -d --name ${env.APP_NAME} '${env.IMAGE}:${env.IMAGETAG}'"
                            }
                            echo "Published Docker image ${env.IMAGE} to GCR"
                        }
                    }
                }
 
                stage('CLEANUP') {
                    steps {
                        script {
                            echo "Cleanup"
                            sh "find . -type f -name config.json -exec /bin/shred {} \\;"
                        }
                    }
                }
            }
        }
    }
 
    def scmGit(scmUrl, branch) {
        return [
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [[url: scmUrl]]
        ]
    }
}
