// shared/utils/PipelineUtils.groovy
package shared.utils

class PipelineUtils {
    static def createPipeline(Map pipelineConfig) {
        echo "Creating pipeline with Git URL: ${pipelineConfig.scmUrl}"

        pipeline {
            agent any
            stages {
                stage('Checkout') {
                    steps {
                        script {
                            // Use the Git URL from the pipelineConfig
                            checkout scm: [
                                $class: 'GitSCM',
                                branches: [[name: pipelineConfig.branch]],
                                userRemoteConfigs: [[url: pipelineConfig.scmUrl]]
                            ]
                        }
                    }
                }

                // Add other stages as needed
            }
        }
    }
}
