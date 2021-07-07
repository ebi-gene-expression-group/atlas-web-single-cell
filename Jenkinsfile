pipeline {
  agent {
    label 'k8s-jdk'
  }

  stages {
    stage('Build') {
      steps {
        checkout scm
        echo 'Checked out!'
      }
    }

    stage('Test') {
      steps {
        container('openjdk') {
          sh 'java --version'
          echo 'Test'
        }
      }
    }

    stage('Deploy') {
      steps {
        echo 'Deploy'
      }
    }

  }
}
