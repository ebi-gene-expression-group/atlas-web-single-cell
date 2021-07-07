pipeline {
  agent {
   label 'k8s-jdk'
  }

  stages {
    stage('Build') {
      steps {
        checkout scm
      }
    }

    stage('Test') {
      steps {
        container('openjdk11')
        sh 'echo $JAVA_HOME && java --version'
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
