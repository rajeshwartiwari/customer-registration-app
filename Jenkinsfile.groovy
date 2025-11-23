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
        
        stage('Install Node.js and npm') {
            steps {
                sh '''#!/bin/bash
                    echo "Installing Node.js and npm..."
                    
                    # Install Node.js 18
                    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
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
        
        stage('Install Docker') {
            steps {
                sh '''#!/bin/bash
                    echo "Installing Docker..."
                    
                    # Update package index
                    apt-get update
                    
                    # Install prerequisites
                    apt-get install -y \\
                        ca-certificates \\
                        curl \\
                        gnupg \\
                        lsb-release
                    
                    # Add Docker's official GPG key
                    mkdir -p /etc/apt/keyrings
                    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
                    
                    # Set up the repository
                    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
                    
                    # Install Docker Engine
                    apt-get update
                    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
                    
                    # Verify installation
                    echo "Docker version:"
                    docker --version
                '''
            }
        }
        
        stage('Install Google Cloud SDK and kubectl') {
            steps {
                sh '''#!/bin/bash
                    echo "Installing Google Cloud SDK and kubectl..."
                    
                    # Install Google Cloud SDK
                    echo "Installing Google Cloud SDK..."
                    curl -sSL https://sdk.cloud.google.com | bash > /dev/null 2>&1
                    source ~/.bashrc
                    
                    # Install kubectl
                    echo "Installing kubectl..."
                    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
                    
                    # Verify installations
                    echo "gcloud version:"
                    gcloud --version
                    echo "kubectl version:"
                    kubectl version --client
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
                        sh """
                            gcloud auth activate-service-account --key-file=${GCP_SA_KEY}
                            gcloud container clusters get-credentials ${GKE_CLUSTER} --zone ${GKE_ZONE} --project ${PROJECT_ID}
                            
                            # Update deployment with new image
                            sed -i 's|IMAGE_PLACEHOLDER|${DOCKER_IMAGE}:${env.BUILD_NUMBER}|g' kubernetes/deployment.yaml
                            
                            kubectl apply -f kubernetes/
                            kubectl rollout status deployment/customer-registration-app --timeout=300s
                        """
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