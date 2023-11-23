// shared/utils/PipelineUtils.groovy
package shared.utils

    class PipelineUtils {
  static def createPipeline(Map pipelineConfig) {
    echo "Creating pipeline with Git URL: ${pipelineConfig.scmUrl}"

        pipeline {
      agent any stages {
        stage('Checkout') {
          steps {
            script {
              // Use the Git URL from the pipelineConfig
              checkout scm
                  : [$class:'GitSCM',
                                 branches:[[name:pipelineConfig.branch]],
                        userRemoteConfigs:[[url:pipelineConfig.scmUrl]]]
            }
          }
          stage('Build Image') {
            steps {
              script { sh 'docker build -t jaydeep .' }
            }
          }

          stage('Access Image Locally') {
            steps {
              script { sh 'docker run -p 8085:3000 jaydeep' }
            }
          }
        }

        // Add other stages as needed
      }
    }
  }
}
