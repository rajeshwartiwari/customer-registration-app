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
        memory: "2Gi"
        cpu: "1000m"
      requests:
        memory: "1Gi"
        cpu: "500m"
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
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
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
'''
        }
    }
    
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
        
        stage('Docker Build') {
            steps {
                container('jnlp') {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE}:${env.BUILD_NUMBER} ."
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                container('jnlp') {
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
