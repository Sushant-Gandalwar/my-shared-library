def call(Map pipelineParams) {
   pipeline{
    agent any 
       parameters {
            choice(name: 'Build_Type', choices: "BUILD&DEPLOY&Publish_to_snapshot\nDEPLOY_ONLY\nPublish_to_Release", description: 'Select the Build type' )
           string(name: 'Parameter', defaultValue: 'default', description: 'Pass the Docker image id if choosed DEPLOY_ONLY OR pass the sbt release command if choosed Publish_to_Release', )
        }        

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            APP_Name = "${pipelineParams.appName}"
            }


        stages {
            stage('INITIALIZE') {
                steps {
                   script{
                     
                    echo "git Initialize env.scmUrl"
                    
                   }
                }
            }
        }
   }
}

