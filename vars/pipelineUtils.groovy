def call(Map pipelineParams) {
   pipeline{
    agent any 
       parameters {
            choice(name: 'Build_Type', choices: "BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release", description: 'Select the Build type' )
           string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release', )
        }        

        environment {
            APP_NAME = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            DB_CREDS_ID = "${pipelineParams.databaseCredentialsId}"
            IMAGE_TAG = "${params.Parameter}"
            COMMAND = "${params.Parameter}"
	        BRANCH =  "${pipelineParams.branch}"
	        EMAIL = "cicd-admin@aienterprise.com"
	        USERNAME = "cicd-admin-aie"
            }


        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        log.info("Initializing environment for webstore delivery pipeline")
                        echo 'Start Initializing!'
                        valuesYaml = loadValuesYaml()
                        git branch: pipelineParams.branch,url: pipelineParams.scmUrl                  
                          if (env.IMAGE_TAG == 'default' && pipelineParams.branch == 'master') {
                          env.PACKAGE_VERSION = sh(
                               script: """
                                 set +x
                                 cat version.sbt | cut -d '=' -f2 | tr -d '\"'
                               """,
                               returnStdout: true
                               ).trim()

                          env.IMAGETAG = 'v' + env.PACKAGE_VERSION + '-' + env.BUILD_NUMBER
                        } else if (env.IMAGE_TAG != 'default' && pipelineParams.branch == 'master') {
                              env.IMAGETAG = env.IMAGE_TAG
                       }  else if (env.IMAGE_TAG == 'default') {
                          env.GIT_COMMIT_ID = sh(
                               script: """
                                 set +x
                                 git log -1 --pretty=%h
                               """,
                               returnStdout: true
                               ).trim()
                          env.IMAGETAG = 'v' + env.GIT_COMMIT_ID + '-' + env.BUILD_NUMBER                       
                       
                       }  else {
                          env.IMAGETAG = env.IMAGE_TAG
                        }                        
                    }
                }
            }
        }
   }
}

def loadValuesYaml() {

         def valuesYaml = readYaml (file: '/var/lib/jenkins/devops/env_properties.yaml')
  	 return valuesYaml;
}
