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
                    echo "=== Installing Node.js 21 ==="
                    
                    if [ ! -f "node-v21.7.3-linux-x64/bin/node" ]; then
                        echo "Downloading Node.js..."
                        curl -fsSL https://nodejs.org/dist/v21.7.3/node-v21.7.3-linux-x64.tar.gz -o node.tar.gz
                        tar -xzf node.tar.gz
                        rm node.tar.gz
                    fi
                    
                    echo "Node.js: $(./node-v21.7.3-linux-x64/bin/node --version)"
                    echo "✅ Node.js installation complete"
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    echo "=== Installing all dependencies ==="
                    
                    # Add Node.js to PATH
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    # Install all dependencies (including devDependencies)
                    npm install --prefer-offline --no-audit --no-fund
                    
                    echo "✅ Dependencies installed successfully!"
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    echo "=== Running tests ==="
                    
                    # Add Node.js to PATH
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    # Run tests
                    npm test
                    
                    echo "✅ Tests completed!"
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "=== Building application ==="
                    
                    # Add Node.js to PATH
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    # Build application
                    npm run build
                    
                    echo "✅ Build completed successfully!"
                '''
            }
        }
        
        stage('Install Docker') {
            steps {
                sh '''#!/bin/bash
                    echo "=== Installing Docker ==="
                    
                    # Update package list
                    apt-get update
                    
                    # Install Docker
                    apt-get install -y docker.io
                    
                    # Start Docker service
                    service docker start
                    
                    # Verify Docker installation
                    docker --version
                    
                    echo "✅ Docker installed successfully!"
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
    }
    
    post {
        always {
            echo "Build ${env.BUILD_NUMBER} completed"
            cleanWs()
        }
        success {
            echo "🎉 SUCCESS: All stages completed successfully!"
        }
        failure {
            echo "❌ FAILED: Build failed"
        }
    }
}
