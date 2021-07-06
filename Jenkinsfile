pipeline {
  agent {
    kubernetes {
      label 'k8s-gradle'
      yaml '''
      spec:
        containers:
        - name: openjdk11
          image: openjdk:11
'''
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
