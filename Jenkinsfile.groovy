pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'rajeshwartiwari/customer-registration-app'
        GKE_CLUSTER = 'demo-gke'
        GKE_ZONE = 'asia-south1-c'
        PROJECT_ID = 'teg-cloud-bfsi-uk1'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Verify Tools') {
            steps {
                sh '''
                    echo "Checking available tools..."
                    node --version || echo "Node.js not found"
                    npm --version || echo "npm not found"
                    docker --version || echo "Docker not found"
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh 'npm ci'
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'npm test'
            }
        }
        
        stage('Build') {
            steps {
                sh 'npm run build'
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:${env.BUILD_NUMBER} ."
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        docker.image("${DOCKER_IMAGE}:${env.BUILD_NUMBER}").push()
                    }
                }
            }
        }
        
        stage('Deploy to GKE') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gke-service-account', variable: 'GCP_SA_KEY')]) {
                        sh '''#!/bin/bash
                            # Install Google Cloud SDK if not available
                            if ! command -v gcloud &> /dev/null; then
                                echo "Installing Google Cloud SDK..."
                                curl -sSL https://sdk.cloud.google.com | bash
                                source ~/.bashrc
                            fi
                            
                            # Install kubectl if not available
                            if ! command -v kubectl &> /dev/null; then
                                echo "Installing kubectl..."
                                KUBECTL_VERSION=\\$(curl -L -s https://dl.k8s.io/release/stable.txt)
                                curl -LO "https://dl.k8s.io/release/\\${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
                                install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
                            fi
                            
                            gcloud auth activate-service-account --key-file=''' + GCP_SA_KEY + '''
                            gcloud container clusters get-credentials ''' + env.GKE_CLUSTER + ''' --zone ''' + env.GKE_ZONE + ''' --project ''' + env.PROJECT_ID + '''
                            
                            # Update deployment with new image
                            sed -i 's|IMAGE_PLACEHOLDER|''' + env.DOCKER_IMAGE + ''':''' + env.BUILD_NUMBER + '''|g' kubernetes/deployment.yaml
                            
                            kubectl apply -f kubernetes/
                            kubectl rollout status deployment/customer-registration-app --timeout=300s
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo "Build ${env.BUILD_NUMBER} completed - cleaning workspace"
            cleanWs()
        }
        success {
            echo "SUCCESS: Customer Registration App ${env.BUILD_NUMBER} deployed to GKE"
        }
        failure {
            echo "FAILED: Customer Registration App ${env.BUILD_NUMBER} deployment failed"
        }
    }
}