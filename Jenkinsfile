pipeline {
  agent {
    kubernetes {
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
