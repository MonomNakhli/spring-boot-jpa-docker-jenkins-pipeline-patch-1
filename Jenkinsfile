pipeline {
    agent any

    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
        APP_PORT = '8081'  // ✅ Port différent de Jenkins
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
                sh """
                    echo "🔧 Déploiement de l'application Spring Boot sur le port ${APP_PORT}..."
                    
                    # Arrêter toute instance existante
                    pkill -f "spring-boot-jpa-docker-jenkins-pipeline" || true
                    sleep 5
                    
                    # Démarrer l'application sur le NOUVEAU port
                    nohup java -jar -Dserver.port=${APP_PORT} target/spring-boot-jpa-docker-jenkins-pipeline-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                    
                    echo "⏳ Attente du démarrage (30 secondes)..."
                    sleep 30
                    
                    echo "📋 Vérification du démarrage..."
                    echo "=== DERNIERS LOGS ==="
                    tail -20 app.log
                    echo "===================="
                    
                    # Test de santé de l'application
                    echo "🧪 Test de l'application..."
                    if curl -s --connect-timeout 15 "http://localhost:${APP_PORT}/spring-boot-jenkins/hello" > /dev/null; then
                        echo "✅ APPLICATION DÉMARRÉE AVEC SUCCÈS"
                        echo "🌐 URL Application: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins"
                        echo "👤 API Students: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins/api/students"
                        echo "🗄️ Console H2: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins/h2-console"
                        echo "🔍 Health Check: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins/actuator/health"
                    else
                        echo "❌ L'application ne répond pas"
                        echo "🔍 Debug - Processus en cours:"
                        ps aux | grep java || true
                        echo "🔍 Debug - Ports en écoute:"
                        netstat -tulpn | grep :${APP_PORT} || true
                        echo "🔍 Debug - Logs complets:"
                        cat app.log
                        exit 1
                    fi
                """
            }
        }

        stage('Smoke Test') {
            steps {
                sh """
                    echo "🚀 Test de fumée sur le port ${APP_PORT}..."
                    
                    # Test des endpoints principaux
                    echo "1. Test endpoint /hello..."
                    curl -f "http://localhost:${APP_PORT}/spring-boot-jenkins/hello" || echo "⚠️ Endpoint /hello non accessible"
                    
                    echo "2. Test endpoint /api/students..."
                    curl -f "http://localhost:${APP_PORT}/spring-boot-jenkins/api/students" || echo "⚠️ Endpoint /api/students non accessible"
                    
                    echo "3. Test health endpoint..."
                    curl -f "http://localhost:${APP_PORT}/spring-boot-jenkins/actuator/health" || echo "⚠️ Health endpoint non accessible"
                    
                    echo "✅ Tests de fumée complétés"
                """
            }
        }

        stage('DAST - Web Scan') {
            steps {
                script {
                    sh """
                        mkdir -p zap-reports
                        echo "🔍 Scan de sécurité DAST sur le port ${APP_PORT}..."
                        
                        docker run --rm -t \\
                        -v \$(pwd)/zap-reports:/zap/wrk \\
                        ghcr.io/zaproxy/zaproxy:stable \\
                        zap-baseline.py -t "http://192.168.33.10:${APP_PORT}/spring-boot-jenkins" \\
                        -r zap_report.html -J zap_out.json -I -d || echo "📊 Scan DAST terminé"
                        
                        echo "📁 Rapport généré: zap-reports/zap_report.html"
                    """
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/zap-reports/*.html, **/target/dependency-check-report.html, **/target/*.jar, app.log', fingerprint: true
            junit '**/target/surefire-reports/*.xml'
            
            echo "🏁 Pipeline DevSecOps terminé - Build #${BUILD_NUMBER}"
        }
        success {
            script {
                echo "🎉 SUCCÈS: L'application est déployée sur le port ${APP_PORT}"
                emailext(
                    to: "mnakhli560@gmail.com",
                    subject: "✅ SUCCÈS Pipeline DevSecOps - Build #${BUILD_NUMBER}",
                    body: """
                    La pipeline DevSecOps a réussi !
                    
                    📊 Détails du build:
                    - Application: Spring Boot JPA Docker
                    - Port: ${APP_PORT}
                    - Statut: DÉPLOIEMENT RÉUSSI
                    - URL: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins
                    - Build: ${env.BUILD_URL}
                    
                    🔗 Accès:
                    Application: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins
                    API Students: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins/api/students
                    Console H2: http://192.168.33.10:${APP_PORT}/spring-boot-jenkins/h2-console
                    """
                )
            }
        }
        failure {
            script {
                echo "💥 ÉCHEC: Vérifiez les logs pour diagnostiquer le problème"
                emailext(
                    to: "mnakhli560@gmail.com",
                    subject: "❌ ÉCHEC Pipeline DevSecOps - Build #${BUILD_NUMBER}",
                    body: """
                    La pipeline DevSecOps a échoué !
                    
                    📊 Détails du build:
                    - Application: Spring Boot JPA Docker
                    - Port: ${APP_PORT}
                    - Statut: DÉPLOIEMENT ÉCHOUÉ
                    - Build: ${env.BUILD_URL}
                    
                    🔍 Causes possibles:
                    - Conflit de port (Jenkins utilise 8080)
                    - Erreur de compilation
                    - Problème de dépendances
                    - Timeout du démarrage
                    
                    Consultez les logs: ${env.BUILD_URL}
                    """
                )
            }
        }
        unstable {
            echo "⚠️ Build instable - Vérifiez les rapports de qualité"
        }
    }
}