pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'rajeshwartiwari/customer-registration-app'
        GKE_CLUSTER = 'demo-gke'
        GKE_ZONE = 'asia-south1-c'
        PROJECT_ID = 'teg-cloud-bfsi-uk1'
        NODE_HOME = "${WORKSPACE}/node-v21.7.3-linux-x64"
        PATH = "${WORKSPACE}/node-v21.7.3-linux-x64/bin:${env.PATH}"
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
                    echo "=== Installing Node.js 21 ==="
                    
                    # Check if Node.js is already installed
                    if [ -d "node-v21.7.3-linux-x64" ]; then
                        echo "Node.js already installed, skipping download..."
                    else
                        echo "Downloading Node.js..."
                        curl -fsSL https://nodejs.org/dist/v21.7.3/node-v21.7.3-linux-x64.tar.gz -o node.tar.gz
                        
                        echo "Extracting Node.js..."
                        tar -xzf node.tar.gz
                        rm node.tar.gz
                    fi
                    
                    # Verify installation
                    echo "Node.js version:"
                    ./node-v21.7.3-linux-x64/bin/node --version
                    echo "npm version:"
                    ./node-v21.7.3-linux-x64/bin/npm --version
                    
                    echo "=== Node.js installation complete ==="
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    echo "Installing dependencies..."
                    ./node-v21.7.3-linux-x64/bin/npm ci
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    echo "Running tests..."
                    ./node-v21.7.3-linux-x64/bin/npm test
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "Building application..."
                    ./node-v21.7.3-linux-x64/bin/npm run build
                    echo "Build completed successfully!"
                '''
            }
        }
        
        stage('Build with Cloud Build') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gke-service-account', variable: 'GCP_SA_KEY')]) {
                        sh '''
                            # Install gcloud in user space
                            if ! command -v gcloud &> /dev/null; then
                                echo "Installing Google Cloud SDK..."
                                curl -sSL https://sdk.cloud.google.com | bash -s -- --disable-prompts > /dev/null 2>&1
                                source ~/.bashrc
                            fi
                            
                            # Authenticate
                            gcloud auth activate-service-account --key-file=${GCP_SA_KEY}
                            
                            # Build and push using Cloud Build (no Docker required locally)
                            gcloud builds submit --tag gcr.io/${PROJECT_ID}/${DOCKER_IMAGE}:${env.BUILD_NUMBER} .
                        '''
                    }
                }
            }
        }
        
        stage('Deploy to GKE') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gke-service-account', variable: 'GCP_SA_KEY')]) {
                        sh '''
                            source ~/.bashrc
                            gcloud container clusters get-credentials ${GKE_CLUSTER} --zone ${GKE_ZONE} --project ${PROJECT_ID}
                            
                            # Update deployment with GCR image
                            sed -i 's|IMAGE_PLACEHOLDER|gcr.io/${PROJECT_ID}/${DOCKER_IMAGE}:${env.BUILD_NUMBER}|g' kubernetes/deployment.yaml
                            
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