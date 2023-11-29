// common.groovy
def call(Map pipelineParams) {
 
    pipeline {

        agent any
        stages {

            stage('INITIALIZE') {

                steps {

                    script {

                        log.info("Initializing environment for webstore delivery pipeline")

                        echo 'Start Initializing!'

                        valuesYaml = loadValuesYaml()

                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl                  

                    }

                }

            }
        }
    }
}
        
