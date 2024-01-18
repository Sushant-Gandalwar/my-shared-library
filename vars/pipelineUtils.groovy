def call(Map pipelineParams) {
   pipeline{
    agent any 
    parameters{
        choice  (name:'Build_Type',choices:"BUILD&DEPLOY&PUBLISH")
    }
   }
}
