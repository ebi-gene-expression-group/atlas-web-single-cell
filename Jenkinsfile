pipeline {
  agent {
    label 'k8s-gradle'
  }

  stages {
    stage('Build') {
      steps {
        sh './gradlew :app:clean'
      }
    }

    stage('Test') {
      steps {
        sh './gradlew ' +
                '-PdataFilesLocation=/nfs/ma/home/atlas-it/atlas-test-data ' +
                '-PexperimentFilesLocation=/nfs/ma/home/atlas-it/atlas-test-data/scxa ' +
                '-PzkHost=lime.ebi.ac.u ' +
                '-PsolrHost=lime.ebi.ac.uk :app:testClasses'
        sh './gradlew :app:test --tests *Test'
      }
    }

    stage('Deploy') {
      steps {
        echo 'Deploying....'
      }
    }

  }
}
