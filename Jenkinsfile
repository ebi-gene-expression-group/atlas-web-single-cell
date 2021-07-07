pipeline {
  agent {
    label 'k8s-jdk'
  }

  stages {
    stage('Build') {
      steps {
        container('openjdk') {
          sh './gradlew :app:testClasses'
        }
      }
    }

    stage('Test') {
      steps {
        container('openjdk') {
          sh './gradlew :app:test --tests *Test'
          sh './gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
          sh './gradlew -PtestResultsPath=e2e :app:test --tests *WIT'
        }
      }
    }

//    stage('Deploy') {
//      steps {
//        echo 'Deploy'
//      }
//    }

  }
}
