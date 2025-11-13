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

        stage('Gitleaks Scan') {
            steps {
                sh '''
                    docker run --rm -v $WORKSPACE:/src zricethezav/gitleaks:latest detect --source /src --exit-code 0
                '''
            }
        }

        stage('DAST - Web Scan') {
            steps {
                script {
                    sh '''
                        mkdir -p zap-reports
                        chmod 777 zap-reports
                        docker run --rm -t \
                        -v $(pwd)/zap-reports:/zap/wrk \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py -t http://192.168.33.10:8081 \
                        -r zap_report.html -J zap_out.json -I -d
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    pkill -f "demo-0.0.1-SNAPSHOT.jar" || true
                    nohup java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081 > app.log 2>&1 &
                    sleep 10
                    echo "Application Spring Boot demarree"
                    echo "Disponible sur: http://192.168.33.10:8081"
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/zap-reports/*.html, **/target/dependency-check-report.html, app.log', fingerprint: true
            echo 'Pipeline DevSecOps termine'
        }
        success {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "Pipeline DevSecOps reussi : ${currentBuild.fullDisplayName}",
                body: "La pipeline DevSecOps a reussi. Consultez les logs: ${env.BUILD_URL}"
            )
        }
        failure {
            emailext(
                to: "mnakhli560@gmail.com",
                subject: "Pipeline DevSecOps echoue : ${currentBuild.fullDisplayName}",
                body: "Le pipeline a echoue. Consultez les logs: ${env.BUILD_URL}"
            )
        }
    }
}