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
                    
                    # Check if Node.js is already installed and working
                    if [ -f "node-v21.7.3-linux-x64/bin/node" ] && [ -x "node-v21.7.3-linux-x64/bin/node" ]; then
                        echo "Node.js already installed, checking if it works..."
                        if ./node-v21.7.3-linux-x64/bin/node --version > /dev/null 2>&1; then
                            echo "Node.js is working properly"
                        else
                            echo "Node.js exists but not working, re-downloading..."
                            rm -rf node-v21.7.3-linux-x64 node.tar.gz
                        fi
                    fi
                    
                    if [ ! -d "node-v21.7.3-linux-x64" ]; then
                        echo "Downloading Node.js..."
                        
                        # Try different architectures if x64 doesn't work
                        ARCH=$(uname -m)
                        echo "Detected architecture: $ARCH"
                        
                        if [ "$ARCH" = "x86_64" ]; then
                            echo "Downloading x64 version..."
                            curl -fsSL https://nodejs.org/dist/v21.7.3/node-v21.7.3-linux-x64.tar.gz -o node.tar.gz
                            tar -xzf node.tar.gz
                        elif [ "$ARCH" = "aarch64" ]; then
                            echo "Downloading ARM64 version..."
                            curl -fsSL https://nodejs.org/dist/v21.7.3/node-v21.7.3-linux-arm64.tar.gz -o node.tar.gz
                            tar -xzf node.tar.gz
                        else
                            echo "Unknown architecture, trying x64..."
                            curl -fsSL https://nodejs.org/dist/v21.7.3/node-v21.7.3-linux-x64.tar.gz -o node.tar.gz
                            tar -xzf node.tar.gz
                        fi
                        
                        rm -f node.tar.gz
                        
                        # Make binaries executable
                        chmod +x node-v21.7.3-linux-x64/bin/*
                        chmod +x node-v21.7.3-linux-arm64/bin/* 2>/dev/null || true
                    fi
                    
                    # Verify installation properly
                    echo "=== Verifying Node.js installation ==="
                    echo "Current directory: $(pwd)"
                    echo "Node.js directory contents:"
                    ls -la node-*/bin/ 2>/dev/null || ls -la
                    
                    # Set PATH for current session to use the installed Node.js
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    echo "Updated PATH: $PATH"
                    
                    # Try to run node
                    echo "Checking Node.js installation..."
                    node --version || (echo "Node.js not in PATH"; exit 1)
                    npm --version || (echo "npm not in PATH"; exit 1)
                    
                    echo "✅ Node.js installation verified!"
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    echo "=== Installing dependencies ==="
                    
                    # Add Node.js to PATH for this stage
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    echo "Node.js version:"
                    node --version
                    echo "npm version:"
                    npm --version
                    
                    echo "Installing dependencies..."
                    npm install
                    
                    echo "✅ Dependencies installed successfully!"
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    echo "=== Running tests ==="
                    
                    # Add Node.js to PATH for this stage
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    echo "Running tests..."
                    npm test
                    
                    echo "✅ Tests completed successfully!"
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "=== Building application ==="
                    
                    # Add Node.js to PATH for this stage
                    export PATH="$(pwd)/node-v21.7.3-linux-x64/bin:${PATH}"
                    
                    echo "Building application..."
                    npm run build
                    
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
        success {
            echo "🎉 SUCCESS: All stages completed successfully!"
        }
        failure {
            echo "❌ FAILED: Build failed"
        }
    }
}
