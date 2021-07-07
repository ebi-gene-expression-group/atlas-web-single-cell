pipeline {
  agent {
    kubernetes {
      inheritFrom 'jdk11'
      yaml '''
      spec:
        containers:
        - name: openjdk
          image: openjdk:11
'''
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
