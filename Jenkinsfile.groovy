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
                    echo "Installing Node.js 21 without root privileges..."
                    
                    # Download and install Node.js in user space
                    NODE_VERSION="21.7.3"
                    cd /home/jenkins
                    
                    # Download Node.js binary
                    curl -fsSL https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz -o node.tar.xz
                    
                    # Extract to user directory
                    tar -xf node.tar.xz
                    mv node-v${NODE_VERSION}-linux-x64 nodejs
                    
                    # Add to PATH
                    export PATH="/home/jenkins/nodejs/bin:$PATH"
                    echo 'export PATH="/home/jenkins/nodejs/bin:$PATH"' >> ~/.bashrc
                    
                    # Verify installation
                    echo "Node.js version:"
                    node --version
                    echo "npm version:"
                    npm --version
                    
                    # Clean up
                    rm node.tar.xz
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    export PATH="/home/jenkins/nodejs/bin:$PATH"
                    npm ci
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    export PATH="/home/jenkins/nodejs/bin:$PATH"
                    npm test
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    export PATH="/home/jenkins/nodejs/bin:$PATH"
                    npm run build
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