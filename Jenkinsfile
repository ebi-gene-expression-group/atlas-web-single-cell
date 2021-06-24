pipeline {
  agent {
    kubernetes {
      label 'k8s-gradle'
    }
  }

  stages {
    stage('Build') {
      steps {
        checkout([$class: 'GitSCM',
                  extensions: [[$class: 'SubmoduleOption',
                                disableSubmodules: false,
                                recursiveSubmodules: true]]])
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
