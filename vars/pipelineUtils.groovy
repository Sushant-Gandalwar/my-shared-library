// common.groovy
class pipelineUtils 
{
    def log
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
                        git branch: pipelineParams.branch, url: pipelineParams.scmUrl
                    }
                }
            }

            stage('BUILD') {
                when {
                    expression { params.Build_Type == 'BUILD_ONLY' }
                }
                steps {
                    script {
                        echo "Building and running the image"
                    }
                    sh '''
                        Build Image
                    '''
                        sh 'docker build -t sushantgandalwar/jaydeep .'
                        sh 'docker run -p 8085:80 sushantgandalwar/jaydeep'
                }
                post {
                    failure {
                        script {
                            echo "Build or Run failed for Service: ${APP_NAME}"
                        }
                    }
                }
            }
        }
    }
 }
}


        
            
def intilizae()
{
    stage('INITIALIZE') 
           {
                steps
                {
                    script 
                    {
                        log.info("Initializing environment for webstore delivery pipeline")
                        echo 'Start Initializing!'
                        valuesYaml = loadValuesYaml()
                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl                  
                    }
                }
            }
}




def buildDockerImage(imageName, dockerfile) {
    stage('Build Docker Image') {
        steps {
            script {
                sh "docker build -t $imageName -f $dockerfile ."
            }
        }
    }
}

def runDockerContainer(portMapping, imageName) {
    stage('Run Docker Container') {
        steps {
            script {
                sh "docker run -p $portMapping $imageName"
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
