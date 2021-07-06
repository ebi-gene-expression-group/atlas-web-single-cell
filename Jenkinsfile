pipeline {
  agent {
    kubernetes {
      inheritFrom 'k8s-gradle'
    }
  }

  stages {
    stage('Build') {
      steps {
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
