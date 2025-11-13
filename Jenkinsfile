pipeline {
    agent any
    tools {
        maven 'M2_HOME'
        jdk 'JAVA_HOME'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'patch-1', url: 'https://github.com/MonomNakhli/spring-boot-jpa-docker-jenkins-pipeline.git'
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "🔨 Construction de l'application..."
                    mvn clean package -DskipTests
                    
                    echo "📂 Contenu du dossier target :"
                    ls -la target/
                    
                    echo "📦 Fichiers JAR :"
                    ls -la target/*.jar || echo "❌ AUCUN JAR TROUVÉ"
                '''
            }
        }

        stage('Vérification') {
            steps {
                sh '''
                    # Vérifier si le JAR existe
                    if [ ! -f target/*.jar ]; then
                        echo "❌ ERREUR CRITIQUE : Aucun fichier JAR créé !"
                        echo "📋 Causes possibles :"
                        echo "   - Erreur de compilation Maven"
                        echo "   - Problème de dépendances"
                        echo "   - Fichier pom.xml incorrect"
                        exit 1
                    else
                        echo "✅ JAR trouvé :"
                        ls -la target/*.jar
                    fi
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    JAR_FILE=$(ls target/*.jar | head -1)
                    echo "🚀 Déploiement de : $JAR_FILE"
                    
                    pkill -f "java.*jar" || true
                    sleep 3
                    
                    nohup java -jar -Dserver.port=8081 "$JAR_FILE" > app.log 2>&1 &
                    sleep 20
                    
                    if curl -s http://localhost:8081/spring-boot-jenkins/hello; then
                        echo "✅ SUCCÈS : App déployée sur http://192.168.33.10:8081"
                    else
                        echo "❌ Échec du déploiement"
                        cat app.log
                        exit 1
                    fi
                '''
            }
        }
    }
}