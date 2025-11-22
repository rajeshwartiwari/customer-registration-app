pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'your-registry/customer-registration-app'
        GKE_CLUSTER = 'your-gke-cluster'
        GKE_ZONE = 'your-zone'
        PROJECT_ID = 'your-project-id'
    }
    
    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout') {
            steps {
                withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                    sh 'git clone --branch main --single-branch https://$GITHUB_TOKEN@github.com/rajeshwartiwari/customer-registration-app.git .'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'npm ci'
                sh 'npm test'
            }
            post {
                always {
                    junit '**/test-results.xml'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${env.BUILD_NUMBER}")
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://your-registry', 'docker-credentials') {
                        docker.image("${DOCKER_IMAGE}:${env.BUILD_NUMBER}").push()
                    }
                }
            }
        }
        
        stage('Deploy to GKE') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gke-service-account', variable: 'GCP_SA_KEY')]) {
                        sh """
                            gcloud auth activate-service-account --key-file=${GCP_SA_KEY}
                            gcloud container clusters get-credentials ${GKE_CLUSTER} --zone ${GKE_ZONE} --project ${PROJECT_ID}
                            
                            sed -i 's|IMAGE_PLACEHOLDER|${DOCKER_IMAGE}:${env.BUILD_NUMBER}|g' kubernetes/deployment.yaml
                            
                            kubectl apply -f kubernetes/
                            kubectl rollout status deployment/customer-registration-app
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            slackSend channel: '#deployments', 
                     message: "SUCCESS: Customer Registration App ${env.BUILD_NUMBER} deployed to GKE"
        }
        failure {
            slackSend channel: '#deployments', 
                     message: "FAILED: Customer Registration App ${env.BUILD_NUMBER} deployment failed"
        }
    }
}
