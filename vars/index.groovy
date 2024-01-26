def call(Map pipelineParams) {
    pipeline {
        agent any   
        stages {
            stage('INITIALIZE') {
                steps {
                    script {
                        echo 'Start Initializing!'
                       
                    }
                }
            }
        }
    }
}
