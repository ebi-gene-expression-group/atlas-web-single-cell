pipeline {
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }
  
  agent {
    kubernetes {
      cloud 'gke-autopilot'
      workspaceVolume dynamicPVC(storageClassName: 'premium-rwo', accessModes: 'ReadWriteOnce', requestsSize: '5Gi')
      defaultContainer 'openjdk'
      yamlFile 'jenkins-k8s-pod.yaml'
    }
  }


  stages {
    stage('Scale SolrCloud') {
      steps {
        container('kubectl') {}
      }
    }

    stage('Provision Gradle') {
      options {
        timeout (time: 20, unit: "MINUTES")
      }
      steps {
        sh './gradlew --no-watch-fs'
      }
    }


    stage('–– Core lib ––') {
      stages {
        stage('Compile') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs ' +
                    '-Pflyway.url=jdbc:postgresql://localhost:5432/postgres ' +
                    '-Pflyway.user=postgres ' +
                    '-Pflyway.password=postgres ' +
                    '-Pflyway.locations=filesystem:./schemas/flyway/gxa ' +
                    '-Pflyway.schemas=gxa ' +
                    'flywayMigrate'
            sh './gradlew --no-watch-fs ' +
                    '-PdataFilesLocation=/gxa-test-data ' +
                    '-PexperimentFilesLocation=/gxa-test-data/gxa ' +
                    '-PexperimentDesignLocation=/root/expdesign-rw ' +
                    '-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=gxa ' +
                    '-PjdbcUsername=postgres ' +
                    '-PjdbcPassword=postgres ' +
                    '-PzkHosts=scxa-solrcloud-zookeeper-0.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,scxa-solrcloud-zookeeper-1.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,scxa-solrcloud-zookeeper-2.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181 ' +
                    '-PsolrHosts=http://scxa-solrcloud-0.scxa-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr,http://scxa-solrcloud-1.scxa-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr ' +
                    ':atlas-web-core:testClasses'
          }
        }

        stage('Test') {
          options {
            timeout (time: 2, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs -PtestResultsPath=ut :atlas-web-core:test --tests *Test'
            // sh './gradlew --no-watch-fs -PtestResultsPath=it :atlas-web-core:test --tests *IT'
            sh './gradlew --no-watch-fs :atlas-web-core:jacocoTestReport'
          }
        }
      }
    }


    stage('–– Web app ––') {
      stages {
        stage('Compile') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs ' +
                    '-Pflyway.url=jdbc:postgresql://localhost:5432/postgres ' +
                    '-Pflyway.user=postgres ' +
                    '-Pflyway.password=postgres ' +
                    '-Pflyway.locations=filesystem:./schemas/flyway/scxa ' +
                    '-Pflyway.schemas=scxa ' +
                    '-Pflyway.target=18 ' +
                    'flywayMigrate'
            sh './gradlew --no-watch-fs ' +
                    '-PdataFilesLocation=/test-data ' +
                    '-PexperimentFilesLocation=/test-data/scxa ' +
                    '-PexperimentDesignLocation=/root/expdesign-rw ' +
                    '-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=scxa ' +
                    '-PjdbcUsername=postgres ' +
                    '-PjdbcPassword=postgres ' +
                    '-PzkHosts=scxa-solrcloud-zookeeper-0.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,scxa-solrcloud-zookeeper-1.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,scxa-solrcloud-zookeeper-2.scxa-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181 ' +
                    '-PsolrHosts=http://scxa-solrcloud-0.scxa-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr,http://scxa-solrcloud-1.scxa-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr ' +
                    ':app:testClasses'
          }
        }

        stage('Test') {
          options {
            timeout (time: 2, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs -PtestResultsPath=ut :app:test --tests *Test'
            sh './gradlew --no-watch-fs -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
            //sh './gradlew --no-watch-fs -PtestResultsPath=e2e -PexcludeTests=**/FileDownloadControllerWIT.class :app:test --tests *WIT'
            sh './gradlew --no-watch-fs :app:jacocoTestReport'
          }
        }

        stage('–– Build ––') {
          when { anyOf {
            branch 'develop'
          } }
          stages {
            stage('Provision Node.js build environment') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                // To avoid the unpleasant:
                // Err:4 http://deb.debian.org/debian bullseye-updates InRelease
                //   Connection timed out [IP: 199.232.174.132 80]
                sh 'echo \'APT::Acquire::Retries "10";\' > /etc/apt/apt.conf.d/80-retries'

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
                sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash'
                sh '. ~/.bashrc && nvm install 14 --lts'
                sh '. ~/.bashrc && npm install -g npm-check-updates'
              }
            }

            stage('Update and build ES bundles') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                sh 'if [ env.BRANCH_NAME = "chore/solr8" ]; then WEBPACK_OPTS=-ip; else WEBPACK_OPTS=-i; fi; ' +
                        '. ~/.bashrc && ./compile-front-end-packages.sh ${WEBPACK_OPTS}'
              }
            }

            stage('Assemble WAR file') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                sh './gradlew --no-watch-fs :app:war'
                archiveArtifacts artifacts: 'webapps/gxa#sc.war', fingerprint: true
              }
            }
          }
        }
      }
    }
  }



  post {
    always {
      junit 'atlas-web-core/build/ut/**/*.xml'
      //junit 'atlas-web-core/build/it/**/*.xml'

      junit 'app/build/ut/**/*.xml'
      junit 'app/build/it/**/*.xml'
      // junit 'app/build/e2e/**/*.xml'

      archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
      archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
      archiveArtifacts artifacts: 'app/src/main/webapp/resources/js-bundles/report.html', fingerprint: true, allowEmptyArchive: true
    }
  }
}
