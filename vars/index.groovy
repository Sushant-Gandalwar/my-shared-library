def call(Map pipelineParams) {
    pipeline {
        agent any
        

        environment {
            scmUrl = "${pipelineParams.scmUrl}"
            
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
            stage('build image') 
            {
                steps 
                {
                    script 
                    {
                        sh 'docker build -t jaydeep .'
                        sh 'docker login'
                        sh 'docker tag jaydeep sushantgandalwar/hello-world-html:latest'
                        sh 'docker push sushantgandalwar/hello-world-html:latest'
                    }
                }
            }
            
          
        }
    }
}
