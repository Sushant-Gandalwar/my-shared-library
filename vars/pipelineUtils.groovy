/*
Author: Venu Rachakonda <venu.rachakonda@rackspace.com>

lagomDeliveryPipeline - Shared pipeline for lagom micro Services or similar type of services which depend on `sbt` to build applications

Pipline that checksout a repository from SCM, perform build, test and deploy them to one or more kubernetes clusters.

This library is meant to be included from Jenkinsfile in actual service repository that defines a lagomDeliveryPipeline() block similar to the one listed below.
After defining lagomDeliveryPipeline(), the only other line necessary is at top of Jenkinsfile

   @Library('shared-utils') _

Jenkinsfile will pass all arguments from the block to this pipeline script.

Any Jenkinsfile that have a pattern of using apiGateway or maven build should include this pipeline library and they
must have a lagomDeliveryPipeline() function defined.

Do not uncomment this sample Jenkinsfile.  Instead, copy it into the calling lagom service or sbt service and uncomment it there.

lagomDeliveryPipeline(scmUrl: 'Bitbucket URL for service or application',
                      branch: env.BRANCH_NAME 'No need to change this',
                      bitbucketCredentialsId: 'Credentials ID in Jenkins, able to checkout bitbucket repositories in ARC-BMUSA workspace',
                      appName: 'Application name. e.g. organization-impl',
                      dockerDirectory: 'Directory where dockerfile exists e.g. organization-impl/target/docker/stage',
                      dockerImage: 'gcr.io/brandsmart-usa-tooling/application_name e.g: gcr.io/brandsmart-usa-tooling/organization-impl',
                      dockerCredentialsId: 'docker credentials id e.g.: brandsmart-usa-tooling',
                      databaseCredentialsId: 'this is database credentials Id for Service e.g.:organization-db-creds, NOTE: In jenkins this credentials name should be "ENV-app-db-creds" e.g.:  uat-organization-db-creds',
                      unitTests: false, if unitTests exist this flag should be 'true', or leave 'false',
                      flywayMigrate: true, if flyways exist this flag should be 'true', or leave 'false')
*/

def call(Map pipelineParams) {

    pipeline {
        agent any   
	/* triggers {
		pollSCM 'H/5 * * * *'
	      }*/
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
                        git branch: pipelineParams.branch, credentialsId: pipelineParams.bitbucketCredentialsId, url: pipelineParams.scmUrl                  
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

            stage('BUILD') {
            when {
                expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
            }            
                steps {
                    script {
                        log.info("Running Reload, clean and compile")
                    }
		     sh ''' 
		          java -version 
		       	'''
                    sh "sbt reload && sbt clean && sbt compile"
		   // sh "sbt dependencyTree"
                }
                post {
                  failure {
                    script {
                      log.error("Build and compile failed for Service: ${APP_NAME}")
		         }
                  }
                }
            }

            stage ('TEST') {
                when {
                      expression { pipelineParams.unitTests == true }
                      expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
                }
                steps {
                    script {
                        log.warn("Input for ${APP_NAME} UnitTests is set to 'true' in pipeline. Step will fail if tests doesn't exist")
                        log.info("Executing unit tests")
                    }
                    sh "sbt test"
                }
                post {
                  failure {
                    script {
                      log.error("Unit tests failed for Service: ${APP_NAME}")
                    }
                  }
                }
            }

            stage('PUBLISH IMAGE') {
               when {
                     expression { params.Build_Type == 'BUILD&DEPLOY&Publish_to_snapshot' }
               }                        
                steps {
                    script {
                        log.info("Building docker image and publishing to GCR")
                    }
                    sh "sbt publish"
                    sh "sbt docker:publishLocal"
                    withDockerRegistry([credentialsId: "gcr:${env.CREDENTIALS_ID}", url: "https://gcr.io"]) {
                      sh "cd ${env.DOCKERDIRECTORY} && docker build -t '${env.IMAGE}:${env.IMAGETAG}' -f Dockerfile ."
                      sh """
                         docker push '${env.IMAGE}:${env.IMAGETAG}'
                         docker rmi '${env.IMAGE}:${env.IMAGETAG}'
                         
                         """
                    }


                    script {
                        log.info("Published Docker image ${env.IMAGE} to GCR")
                    }
                }
            }

            stage('ARC-DEV APPROVAL') {
                 when {
                    expression { pipelineParams.branch != 'master' }
                }                  
                steps {
                    script {
                        log.warn("Approval is required to perform deployment in DEV, Click 'Proceed or Abort'")
                    }

                    timeout(time: 2, unit: 'HOURS') {
		      verifybuild()
                    }
                }               
            }

            stage('ARC-DEV FLYWAY') {
                when {
                    expression { pipelineParams.flywayMigrate == true }
                    expression { return env.releaseskip == 'dorelease' }
                }
                environment {
                    ENV = "arcdev"
                    APP_DB_CREDS_ID = "${ENV}-${DB_CREDS_ID}"
                    DB_HOST = "${valuesYaml.environments."${ENV}".db_host}"
                }
                steps { 
                    /*catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        log.info("Checking if flyway exists for ${APP_NAME}")
                        flyway()

                    }
                  }*/
		   script {
                        log.info("Checking if flyway exists for ${APP_NAME}")
                       flyway()
                    }
                }

                post {
                    failure {
                        script {
                        log.error("${ENV} flyway migrate failed")
			   }
                    }
                }
            }

            stage ('ARC-DEV DEPLOY') {
                when {
                    expression { pipelineParams.flywayMigrate == true }
                    expression { return env.releaseskip == 'dorelease' }
                } 
                environment {
                    ENV = "arcdev"
                    DB_HOST = "${valuesYaml.environments."${ENV}".db_host}"
		    DB_HOST_IP = "${valuesYaml.environments."${ENV}".db_host_ip}"
		    DB_HOST_PORT = "${valuesYaml.environments."${ENV}".db_host_port}"
                    KAFKA_HOST_IP = "${valuesYaml.environments."${ENV}".kafka_host_ip}"
                    CLUSTER_NAME = "${valuesYaml.environments."${ENV}".cluster_name}"
                    LOCATION = "${valuesYaml.environments."${ENV}".location}"
                    PROJECT_ID = "${valuesYaml.environments."${ENV}".project_id}"
                    LIMITS_CPU = "${valuesYaml.environments."${ENV}".limits_cpu}"
                    LIMITS_MEMORY = "${valuesYaml.environments."${ENV}".limits_memory}"
                    REQUEST_CPU = "${valuesYaml.environments."${ENV}".request_cpu}"
                    REQUEST_MEMORY = "${valuesYaml.environments."${ENV}".request_memory}"
                    MIN_REPLICAS = "${valuesYaml.environments."${ENV}".min_replicas}"
                    ELASTICSEARCH_HOST_URL_LIST = "${valuesYaml.environments."${ENV}".elasticsearch_host}"
		    CONTENT_REPLICAS = "${valuesYaml.environments."${ENV}".min_content_replicas}"
		    INITIALDELAY_SECONDS = "${valuesYaml.environments."${ENV}".initialDelay_seconds}"
		    PERIOD_SECONDS = "${valuesYaml.environments."${ENV}".period_seconds}"
                    FAILURE_THRESHOLD = "${valuesYaml.environments."${ENV}".failure_threshold}"
		   
                }
                steps {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    script {
                        log.info("Triggering deployment for service in ${ENV} kubernetes environment")
                        deploy()
			  }
                  }
                }
                post {
                    failure {
                        script {
                            log.error("Deployment failed in ${ENV} environment")
                        }
                    }
                }
            }

           stage ('RELEASE') {
			
		when {
                      expression { params.Build_Type == 'Publish_to_Release' }
                }
			
                steps {
                    script {
                        log.info("Executing sbt release")
			log.info("Initializing environment for webstore delivery pipeline : ${params.Parameter}")

                    }
			sh "git checkout ${BRANCH}"
		   	sh "git config remote.origin.fetch +refs/heads/*:refs/remotes/origin/*"
		   	sh "git config branch.${BRANCH}.remote origin"
		   	sh "git config branch.${BRANCH}.merge refs/heads/${BRANCH}"
   		   	sh "git remote rm origin"
		   	sh "git config user.name ${USERNAME}"
		   	sh "git config user.email ${EMAIL}"
   		   	sh "git remote add origin https://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@github.com/RETISIO/arc-schema-evaluator.git"
		   	sh "git push --set-upstream origin ${BRANCH}"
		   	sh "${COMMAND}"
		   	sh "git push"

                }
                post {
                  failure {
                    script {
                      log.error("sbt release failed for the service")
                    }
                  }
                }
            }	
           stage ('CLEANUP') {
                steps {
                    script {
                        log.info("Cleanup")
                        sh "find . -type f -name config.json -exec /bin/shred {} \\;"
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

def verifybuild() {

        def userInput = input(
            id: 'userInput', message: 'Approve Deployment!',        parameters: [

      [$class: 'BooleanParameterDefinition', defaultValue: 'false', description: 'click to skip', name: 'skip'],
    ])

        if(!userInput) {
            env.releaseskip = 'dorelease'
            }
            else {
                env.releaseskip = 'norelease'

            }

    }
