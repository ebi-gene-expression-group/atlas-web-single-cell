pipeline {
  agent {
    // Remember to add a Pod Template with label 'k8s-jdk' and container templates to match the stages
    label 'k8s-jdk'
  }



  stages {
    stage('Prepare environment') {
      options {
        timeout (time: 5, unit: "MINUTES")
      }
      steps {
        container('openjdk') {
          sh 'echo Download Gradle and start daemon...'
          sh './gradlew'
        }
      }
    }


    stage('Core lib') {
      stages {
        stage('Compile') {
          options {
            timeout (time: 5, unit: "MINUTES")
          }
          steps {
            container('openjdk') {
              sh './gradlew ' +
                      '-PdataFilesLocation=/test-data ' +
                      '-PexperimentFilesLocation=/test-data/scxa ' +
                      '-PjdbcUrl=jdbc:postgresql://localhost:5432/scxa ' +
                      '-PjdbcUsername=scxa ' +
                      '-PjdbcPassword=scxa ' +
                      '-PzkHost=scxa-zk.scxa-test.svc.cluster.local ' +
                      '-PzkPort=2181 ' +
                      '-PsolrHost=scxa-solrcloud.scxa-test.svc.cluster.local ' +
                      '-PsolrPort=8983 ' +
                      ':atlas-web-core:testClasses'
            }
          }
        }

        stage('Test') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            container('openjdk') {
              // sh './gradlew -PtestResultsPath=ut :atlas-web-core:test --tests *Test'
              // sh './gradlew -PtestResultsPath=it :atlas-web-core:test --tests *IT'
              // sh './gradlew :atlas-web-core:jacocoTestReport'
            }
          }
        }
      }
    }


    stage('Web app') {
      stages {
        stage('Compile') {
          options {
            timeout (time: 5, unit: "MINUTES")
          }
          steps {
            container('openjdk') {
              sh './gradlew ' +
                      '-PdataFilesLocation=/test-data ' +
                      '-PexperimentFilesLocation=/test-data/scxa ' +
                      '-PjdbcUrl=jdbc:postgresql://localhost:5432/scxa ' +
                      '-PjdbcUsername=scxa ' +
                      '-PjdbcPassword=scxa ' +
                      '-PzkHost=scxa-zk.scxa-test.svc.cluster.local ' +
                      '-PzkPort=2181 ' +
                      '-PsolrHost=scxa-solrcloud.scxa-test.svc.cluster.local ' +
                      '-PsolrPort=8983 ' +
                      ':app:testClasses'
            }
          }
        }

        stage('Test') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            container('openjdk') {
              sh './gradlew -PtestResultsPath=ut :app:test --tests *Test'
              // sh './gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
              // sh './gradlew -PtestResultsPath=e2e :app:test --tests *WIT'
              sh './gradlew :app:jacocoTestReport'
            }
          }
        }

        stage('Build WAR') {
          options {
            timeout (time: 5, unit: "MINUTES")
          }
//          when { anyOf {
//            branch 'develop'
//            branch 'main'
//          } }
          steps {
            container('openjdk') {
              sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash'
              sh '. ~/.bashrc'
              sh 'nvm install 14'
              sh 'npm install -g ncu'
              sh './update-compile-front-end-packages.sh'
              sh './gradlew :app:war'
              archiveArtifacts artifacts: 'webapps/gxa#sc.war', fingerprint: true
            }
          }
        }
      }
    }
  }



  post {
    always {
      container('openjdk') {
        // junit 'atlas-web-core/build/ut/**/*.xml'
        // junit 'atlas-web-core/build/it/**/*.xml'

        junit 'app/build/ut/**/*.xml'
        // junit 'app/build/it/**/*.xml'
        // junit 'app/build/e2e/**/*.xml'
        archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
        archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
      }
    }
  }
}
