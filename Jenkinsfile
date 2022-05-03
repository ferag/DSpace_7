@Library(['github.com/indigo-dc/jenkins-pipeline-library@release/2.1.0']) _

def projectConfig

pipeline {
    agent any

    stages {
        stage('SQA baseline dynamic stages') {
            steps {
                script {
                    projectConfig = pipelineConfig()
                    buildStages(projectConfig)
                }
            }
            post {
                cleanup {
                    cleanWs()
                }
            }
        }
        stage('Component integration tests') {
            agent any
            steps {
                echo "Running tests in a fully containerized environment - :)"
                dir ('./dspace-angular') {
                    sh 'docker-compose -p d7 -f docker/docker-compose.yml -f docker/docker-compose-rest.yml up -d'
                }
            }
        }
        stage('Check user creation') {
            agent any
            steps {
                echo "Running tests in a fully containerized environment - :)"
                dir ('./dspace-angular') {
                    sh 'docker-compose -p d7 -f docker/cli.yml run --rm dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en'
                }
            }
        }
        stage('Check data ingestion') {
            agent any
            steps {
                echo "Running tests in a fully containerized environment - :)"
                dir ('./dspace-angular') {
                    sh 'docker-compose -p d7 -f docker/cli.yml -f ./docker/cli.ingest.yml run --rm dspace-cli'
                }
            }
        }
        stage('Repository FAIRness assesment') {
            agent any
            steps {
                echo "Check ingestion"
                dir ('.') {
                    sh 'docker logs -f dspace-ingest'
                    sh 'docker logs -f dspace-cli'
                }
            }
        }
        stage('Integration cleanup') {
            agent any
            steps {
                echo "Clean up"
                dir ('.') {
                    sh 'docker-compose -f docker-compose-fair.yml down -v'
                }
            }
        }
    }
}

