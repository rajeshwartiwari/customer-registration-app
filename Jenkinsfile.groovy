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
        
        stage('Install Node.js') {
            steps {
                script {
                    // Install Node.js if not available
                    sh '''
                        if ! command -v node &> /dev/null; then
                            echo "Installing Node.js..."
                            curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
                            apt-get install -y nodejs
                        else
                            echo "Node.js is already installed"
                        fi
                        
                        # Verify installation
                        node --version
                        npm --version
                    '''
                }
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh 'npm ci'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'npm test'
            }
            post {
                always {
                    // Only publish JUnit results if test results exist
                    script {
                        if (fileExists('test-results.xml')) {
                            junit 'test-results.xml'
                        } else {
                            echo 'No test results found, skipping JUnit report'
                        }
                    }
                }
            }
        }
        
        stage('Build Application') {
            steps {
                sh 'npm run build'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    // Check if Docker is available
                    sh 'docker --version'
                    docker.build("${DOCKER_IMAGE}:${env.BUILD_NUMBER}")
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
                            # Install Google Cloud SDK if not available
                            if ! command -v gcloud &> /dev/null; then
                                echo "Installing Google Cloud SDK..."
                                curl https://sdk.cloud.google.com | bash
                                source ~/.bashrc
                            fi
                            
                            # Install kubectl if not available
                            if ! command -v kubectl &> /dev/null; then
                                echo "Installing kubectl..."
                                curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
                                install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
                            fi
                            
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
            // Remove slackSend if plugin not installed, or install Slack plugin
            // slackSend channel: '#deployments', 
            //          message: "SUCCESS: Customer Registration App ${env.BUILD_NUMBER} deployed to GKE"
        }
        failure {
            echo "FAILED: Customer Registration App ${env.BUILD_NUMBER} deployment failed"
            // Remove slackSend if plugin not installed, or install Slack plugin
            // slackSend channel: '#deployments', 
            //          message: "FAILED: Customer Registration App ${env.BUILD_NUMBER} deployment failed"
        }
    }
}