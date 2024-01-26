def call(Map pipelineParams) {

    pipeline {
        agent any   
        parameters {
            choice(name: 'Build_Type', choices: "BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release", description: 'Select the Build type' )
            string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if chosen DEPLOY_ONLY OR pass the sbt release command if chosen Publish_to_Release', )
        }        

        environment {
            APP_NAME = "${pipelineParams.appName}"
            DOCKERDIRECTORY = "${pipelineParams.dockerDirectory}"
            IMAGE = "${pipelineParams.dockerImage}"
            CREDENTIALS_ID = "${pipelineParams.dockerCredentialsId}"
            DB_CREDS_ID = "${pipelineParams.databaseCredentialsId}"
            IMAGE_TAG = "${params.Parameter}"
            COMMAND = "${params.Parameter}"
            BRANCH = "${pipelineParams.branch}"
            EMAIL = "cicd-admin@aienterprise.com"
            USERNAME = "cicd-admin-aie"
        }

        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        // log.info("Initializing environment for webstore delivery pipeline")
                        echo 'Start Initializing!'
                                       
                }
            }
        }
    }
}
