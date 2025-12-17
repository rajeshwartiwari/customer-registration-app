pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: jnlp
    image: jenkins/inbound-agent:3345.v03dee9b_f88fc-6
    resources:
      limits:
        memory: "1Gi"
        cpu: "500m"
      requests:
        memory: "512Mi"
        cpu: "250m"
  - name: node
    image: node:21-alpine
    command: ["cat"]
    tty: true
    resources:
      limits:
        memory: "1Gi"
        cpu: "500m"
      requests:
        memory: "512Mi"
        cpu: "250m"
  - name: dind
    image: docker:dind
    securityContext:
      privileged: true
    args:
    - dockerd
    - --host=tcp://0.0.0.0:2375
    env:
    - name: DOCKER_TLS_CERTDIR
      value: ""
    resources:
      limits:
        memory: "1Gi"
        cpu: "500m"
      requests:
        memory: "512Mi"
        cpu: "250m"
'''
        }
    }
    
    environment {
        DOCKER_IMAGE = 'rajeshwartiwari/customer-registration-app'
        DOCKER_HOST = 'tcp://localhost:2375'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Install Dependencies') {
            steps {
                container('node') {
                    sh '''
                        echo "=== Installing dependencies ==="
                        npm install --prefer-offline --no-audit --no-fund
                        echo "✅ Dependencies installed successfully!"
                    '''
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                container('node') {
                    sh '''
                        echo "=== Running tests ==="
                        npm test
                        echo "✅ Tests completed!"
                    '''
                }
            }
        }
        
        stage('Build') {
            steps {
                container('node') {
                    sh '''
                        echo "=== Building application ==="
                        npm run build
                        echo "✅ Build completed successfully!"
                    '''
                }
            }
        }
        
        stage('Install Docker Client in Node Container') {
            steps {
                container('node') {
                    sh '''
                        echo "=== Installing Docker client ==="
                        apk add --no-cache docker-cli
                        docker --version
                        echo "✅ Docker client installed!"
                    '''
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                container('node') {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE}:${env.BUILD_NUMBER} ."
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                container('node') {
                    script {
                        docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                            docker.image("${DOCKER_IMAGE}:${env.BUILD_NUMBER}").push()
                        }
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
