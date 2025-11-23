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
                    
                    # Try to run node with full path
                    NODE_PATH=$(find . -name node -type f -executable | head -1)
                    if [ -n "$NODE_PATH" ]; then
                        echo "Found node at: $NODE_PATH"
                        $NODE_PATH --version
                        NPM_PATH=$(find . -name npm -type f -executable | head -1)
                        if [ -n "$NPM_PATH" ]; then
                            echo "Found npm at: $NPM_PATH"
                            $NPM_PATH --version
                        else
                            echo "npm not found"
                        fi
                    else
                        echo "Node.js binary not found or not executable"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh '''
                    echo "Installing dependencies..."
                    
                    # Find npm path
                    NPM_PATH=$(find . -name npm -type f -executable | head -1)
                    if [ -n "$NPM_PATH" ]; then
                        echo "Using npm from: $NPM_PATH"
                        $NPM_PATH install
                        echo "✅ Dependencies installed successfully!"
                    else
                        echo "❌ npm not found"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('Run Tests') {
            steps {
                sh '''
                    echo "Running tests..."
                    NPM_PATH=$(find . -name npm -type f -executable | head -1)
                    $NPM_PATH test
                    echo "✅ Tests completed successfully!"
                '''
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "Building application..."
                    NPM_PATH=$(find . -name npm -type f -executable | head -1)
                    $NPM_PATH run build
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