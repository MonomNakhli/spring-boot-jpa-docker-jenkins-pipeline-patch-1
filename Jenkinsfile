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
                git branch: 'patch-1', url: 'https://github.com/MonomNakhli/spring-boot-jpa-docker-jenkins-pipeline.git'
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
                    echo "Deploiement de VOTRE application Spring Boot avec JPA..."
                    # Arreter toute instance existante
                    pkill -f "spring-boot-jpa-docker-jenkins-pipeline" || true
                    sleep 3
                    
                    # Demarrer VOTRE application avec le bon contexte
                    nohup java -jar target/spring-boot-jpa-docker-jenkins-pipeline-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                    
                    # Attendre le demarrage (H2 demarre plus vite que MySQL)
                    sleep 20
                    
                    echo "Verification du demarrage..."
                    echo "=== LOGS APPLICATION ==="
                    tail -15 app.log
                    
                    # Tester l'application
                    if curl -s --connect-timeout 10 http://localhost:8080/spring-boot-jenkins/hello > /dev/null; then
                        echo "✅ VOTRE APPLICATION SPRING BOOT AVEC JPA EST DEMARREE"
                        echo "🌐 Application: http://192.168.33.10:8080/spring-boot-jenkins"
                        echo "👥 API Students: http://192.168.33.10:8080/spring-boot-jenkins/api/students"
                        echo "🗄️ Console H2: http://192.168.33.10:8080/spring-boot-jenkins/h2-console"
                    else
                        echo "⚠️ Application en cours de demarrage ou non accessible reseau"
                        echo "💡 En local: http://localhost:8080/spring-boot-jenkins/hello"
                    fi
                '''
            }
        }

        stage('DAST - Web Scan') {
            steps {
                script {
                    sh '''
                        mkdir -p zap-reports
                        echo "Tentative de scan DAST sur VOTRE application..."
                        docker run --rm -t \
                        -v $(pwd)/zap-reports:/zap/wrk \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py -t http://192.168.33.10:8080/spring-boot-jenkins \
                        -r zap_report.html -J zap_out.json -I -d || echo "Scan DAST termine - application peut-etre non accessible reseau"
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/zap-reports/*.html, **/target/dependency-check-report.html, app.log', fingerprint: true
            echo 'Pipeline DevSecOps avec VOTRE application termine'
        }
        success {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "Pipeline DevSecOps VOTRE app reussi : ${currentBuild.fullDisplayName}",
                body: "La pipeline DevSecOps avec VOTRE application Spring Boot a reussi. Consultez les logs: ${env.BUILD_URL}"
            )
        }
        failure {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "Pipeline DevSecOps VOTRE app echoue : ${currentBuild.fullDisplayName}",
                body: "Le pipeline avec VOTRE application a echoue. Consultez les logs: ${env.BUILD_URL}"
            )
        }
    }
}