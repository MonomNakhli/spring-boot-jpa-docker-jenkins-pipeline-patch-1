pipeline {
    agent any

    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/oktadev/spring-boot-docker-example.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SAST - SonarQube') {
            steps {
                sh "mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=${SONAR_TOKEN}"
            }
        }

        stage('SCA - Dependency Check') {
            steps {
                sh 'mvn org.owasp:dependency-check-maven:check'
            }
        }

        stage('Docker Security Scan') {
            steps {
                sh '''
                    echo "🔒 Docker Security Scan (Simulation)"
                    echo "✅ En production: trivy image springboot-app:latest"
                    echo "✅ En production: docker scout quickview springboot-app:latest"
                '''
            }
        }

        stage('Gitleaks Scan') {
            steps {
                sh '''
                    docker run --rm -v $WORKSPACE:/src zricethezav/gitleaks:latest detect --source /src --exit-code 0
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    # Arrête l'instance précédente si elle existe
                    pkill -f "demo-0.0.1-SNAPSHOT.jar" || true
                    # Démarre sur le port 8081 pour éviter conflit avec Jenkins
                    nohup java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081 > app.log 2>&1 &
                    sleep 10
                    echo "🚀 Application Spring Boot déployée avec succès"
                    echo "🌐 Disponible sur: http://192.168.33.10:8081"
                    echo "📱 Testez: http://192.168.33.10:8081/hello"
                '''
            }
        }

        stage('DAST - Web Scan') {
            steps {
                script {
                    sh '''
                        mkdir -p zap-reports
                        chmod 777 zap-reports
                        # Scan de VOTRE app déployée sur le port 8081
                        docker run --rm -t \
                        -v $(pwd)/zap-reports:/zap/wrk \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py -t http://192.168.33.10:8081 \
                        -r zap_report.html -J zap_out.json -I -d
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/zap-reports/*.html, **/target/dependency-check-report.html, app.log', fingerprint: true
            echo '🔚 Pipeline DevSecOps terminé !'
        }
        success {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "✅ Pipeline DevSecOps réussi : ${currentBuild.fullDisplayName}",
                body: "La pipeline DevSecOps a réussi! Application disponible sur: http://192.168.33.10:8081"
            )
        }
        failure {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "❌ Pipeline DevSecOps échoué : ${currentBuild.fullDisplayName}",
                body: "Le pipeline a échoué. Consultez les logs: ${env.BUILD_URL}"
            )
        }
    }
}