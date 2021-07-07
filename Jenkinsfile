pipeline {
  agent {
    kubernetes {
      inheritFrom 'jdk11'
    }
  }

  stages {
    stage('Build') {
      steps {
        sh 'echo $JAVA_HOME && java --version'
        checkout scm
        sh './gradlew :app:testClasses'
      }
    }

    stage('Test') {
      steps {
        echo 'Test'
      }
    }

    stage('Deploy') {
      steps {
        echo 'Deploy'
      }
    }

  }
}
