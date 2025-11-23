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
        
        stage('Install Node.js 21') {
            steps {
                sh '''#!/bin/bash
                    echo "Installing Node.js 21 using NodeSource..."
                    
                    # Install Node.js 21 without root privileges (user space)
                    NODE_VERSION="21"
                    curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash -
                    apt-get update
                    apt-get install -y nodejs
                    
                    # Verify installation
                    echo "Node.js version:"
                    node --version
                    echo "npm version:"
                    npm --version
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
        
        stage('Install Docker') {
            steps {
                sh '''#!/bin/bash
                    echo "Installing Docker..."
                    
                    # Update package index
                    apt-get update
                    
                    # Install Docker
                    apt-get install -y docker.io
                    
                    # Start Docker service
                    service docker start
                    
                    # Verify installation
                    echo "Docker version:"
                    docker --version
                '''
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
                        sh '''
                            # Install gcloud and kubectl
                            if ! command -v gcloud &> /dev/null; then
                                echo "Installing Google Cloud SDK..."
                                curl -sSL https://sdk.cloud.google.com | bash -s -- --disable-prompts > /dev/null 2>&1
                                source ~/.bashrc
                            fi
                            
                            if ! command -v kubectl &> /dev/null; then
                                echo "Installing kubectl..."
                                curl -LO "https://dl.k8s.io/release/\\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                                chmod +x kubectl
                                mv kubectl /usr/local/bin/
                            fi
                            
                            gcloud auth activate-service-account --key-file=${GCP_SA_KEY}
                            gcloud container clusters get-credentials ${GKE_CLUSTER} --zone ${GKE_ZONE} --project ${PROJECT_ID}
                            
                            sed -i 's|IMAGE_PLACEHOLDER|${DOCKER_IMAGE}:${env.BUILD_NUMBER}|g' kubernetes/deployment.yaml
                            
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