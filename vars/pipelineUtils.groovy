def call(Map pipelineParams) {
   pipeline{
    agent any 
       parameters {
            choice(name: 'Build_Type', choices: "BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release", description: 'Select the Build type' )
           string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release', )
        }        

        environment {
            APP_NAME = "${pipelineParams.appName}"
            IMAGE_TAG = "${params.Parameter}"
            COMMAND = "${params.Parameter}"
	        BRANCH =  "${pipelineParams.branch}"
            }


        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        log.info("Initializing environment for webstore delivery pipeline")
                        echo 'Start Initializing!'
                        git branch: pipelineParams.branch,url: pipelineParams.scmUrl                  
                          if (env.IMAGE_TAG == 'default' && pipelineParams.branch == 'main') {
                          env.PACKAGE_VERSION = sh(
                               script: """
                                 set +x
                                 cat version.sbt | cut -d '=' -f2 | tr -d '\"'
                               """,
                               returnStdout: true
                               ).trim()

                          env.IMAGETAG = 'v' + env.PACKAGE_VERSION + '-' + env.BUILD_NUMBER
                        }                      
                    }
                }
            }
        }
   }
}

