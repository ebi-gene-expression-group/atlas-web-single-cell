pipeline {
  agent {
    // Remember to add a Pod Template named 'k8s-jdk' and container templates to match the stages
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
          echo 'Wait until localhost:5432 is available...'
          sh 'for i in $(seq 1 200); do nc -z -w3 localhost 5432 && exit 0 || sleep 3; done; exit 1'
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
