pipeline {
  agent {
    // Remember to add a Pod Template named 'k8s-jdk' and container templates to match the stages
    label 'k8s-jdk'
  }

  stages {
    stage('Build') {
      steps {
        container('openjdk') {
          sh './gradlew ' +
                  '-PdataFilesLocation=/test-data ' +
                  '-PexperimentFilesLocation=/test-data/scxa ' +
                  '-PzkHost=scxa-zk.scxa-test.svc.cluster.local ' +
                  '-PzkPort=2181 ' +
                  '-PsolrHost=scxa-solrcloud.scxa-test.svc.cluster.local ' +
                  '-PsolrPort=8983 ' +
                  ':app:testClasses'
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
