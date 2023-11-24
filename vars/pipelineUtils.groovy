// common.groovy

def checkoutAndBuildProject(scmUrl, branch, appName, buildCommand) {
    stage('Checkout') {
        steps {
            script {
                checkout scmGit(scmUrl, branch)
                sh buildCommand
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
