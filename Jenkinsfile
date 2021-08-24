pipeline {
  agent {
    // Remember to add a Pod Template with label 'k8s-jdk' and container templates to match the stages
    label 'k8s-jdk'
  }



  stages {
    stage('Provision Gradle and start daemon') {
      options {
        timeout (time: 5, unit: "MINUTES")
      }
      steps {
        container('openjdk') {
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
                      '-PzkHost=scxa-zk-cs.scxa-test.svc.cluster.local ' +
                      '-PzkPort=2181 ' +
                      '-PsolrHost=scxa-solrcloud-cs.scxa-test.svc.cluster.local ' +
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
                      '-Pflyway.url=jdbc:postgresql://localhost:5432/scxa ' +
                      '-Pflyway.user=scxa ' +
                      '-Pflyway.password=scxa ' +
                      '-Pflyway.locations=filesystem:./schemas/flyway/scxa ' +
                      '-Pflyway.schemas=scxa ' +
                      'flywayMigrate'
              sh './gradlew ' +
                      '-PdataFilesLocation=/test-data ' +
                      '-PexperimentFilesLocation=/test-data/scxa ' +
                      '-PjdbcUrl=jdbc:postgresql://localhost:5432/scxa ' +
                      '-PjdbcUsername=scxa ' +
                      '-PjdbcPassword=scxa ' +
                      '-PzkHost=scxa-zk-hs.scxa-test.svc.cluster.local ' +
                      '-PzkPort=2181 ' +
                      '-PsolrHost=scxa-solrcloud-hs.scxa-test.svc.cluster.local ' +
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
              sh './gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
              sh './gradlew -PtestResultsPath=e2e :app:test --tests *WIT'
              sh './gradlew :app:jacocoTestReport'
            }
          }
        }

        stage('Build') {
          when { anyOf {
            branch 'develop'
            branch 'main'
          } }
          stages {
            stage('Provision Node.js build environment') {
              options {
                timeout (time: 5, unit: "MINUTES")
              }
              steps {
                container('openjdk') {
                  // Required by node_modules/cwebp-bin
                  // /home/jenkins/agent/workspace/298051-test-and-build-in-jenkins/app/src/main/javascript/node_modules/cwebp-bin/vendor/cwebp:
                  // error while loading shared libraries: libGL.so.1: cannot open shared object file: No such file or directory
                  //
                  //  ⚠ cwebp pre-build test failed
                  //  ℹ compiling from source
                  //  ✖ Error: Command failed: /bin/sh -c ./configure --disable-shared --prefix="/home/jenkins/agent/workspace/298051-test-and-build-in-jenkins/app/src/main/javascript/bundles/experiment-page/node_modules/cwebp-bin/vendor" --bindir="/home/jenkins/agent/workspace/298051-test-and-build-in-jenkins/app/src/main/javascript/bundles/experiment-page/node_modules/cwebp-bin/vendor"
                  // configure: error: in `/home/jenkins/agent/workspace/298051-test-and-build-in-jenkins/app/src/main/javascript/bundles/experiment-page/node_modules/cwebp-bin/2525557b-9d4c-4886-93b3-8cbfa3b76a32':
                  // configure: error: no acceptable C compiler found in $PATH
                  sh 'apt update && apt install -y libglu1-mesa gcc'
                  sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash'
                  sh '. ~/.bashrc && nvm install 14'
                  sh '. ~/.bashrc && npm install -g ncu'
                }
              }
            }

            stage('Update and build ES bundles') {
              options {
                timeout (time: 15, unit: "MINUTES")
              }
              steps {
                container('openjdk') {
                  sh '[[ env.BRANCH_NAME = main ]] && WEBPACK_OPTS=-p || WEBPACK_OPTS=-d && ' +
                          '. ~/.bashrc && ./update-compile-front-end-packages.sh ${WEBPACK_OPTS}'
                }
              }
            }

            stage('Assemble WAR file') {
              options {
                timeout (time: 5, unit: "MINUTES")
              }
              steps {
                container('openjdk') {
                  sh './gradlew :app:war'
                  archiveArtifacts artifacts: 'webapps/gxa#sc.war', fingerprint: true
                }
              }
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
        junit 'app/build/it/**/*.xml'
        junit 'app/build/e2e/**/*.xml'
        archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
        archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
        archiveArtifacts artifacts: 'app/src/main/webapp/resources/js-bundles/report.html', fingerprint: true, allowEmptyArchive: true
      }
    }
  }
}
