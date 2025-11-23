pipeline {
    agent any
    
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
                        tar -xzf node.tar.gz
                        rm node.tar.gz
                    fi
                    
                    echo "Node.js: $(./node-v21.7.3-linux-x64/bin/node --version)"
                    echo "npm: $(./node-v21.7.3-linux-x64/bin/npm --version)"
                '''
            }
        }
        
        stage('Verify Project Structure') {
            steps {
                sh '''
                    echo "=== Checking project structure ==="
                    ls -la
                    echo "=== Checking package.json ==="
                    cat package.json || echo "No package.json found"
                    echo "=== Checking if package-lock.json exists ==="
                    ls -la package-lock.json || echo "No package-lock.json - will use npm install"
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    echo "Installing dependencies..."
                    if [ -f "package-lock.json" ]; then
                        echo "Using npm ci (package-lock.json exists)"
                        ./node-v21.7.3-linux-x64/bin/npm ci
                    else
                        echo "Using npm install (no package-lock.json)"
                        ./node-v21.7.3-linux-x64/bin/npm install
                    fi
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
                    echo "✅ Build completed successfully!"
                '''
            }
        }
    }
    
    post {
        always {
            echo "Build ${env.BUILD_NUMBER} completed"
            cleanWs()
        }
    }
}