pipeline {
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    lock(resource: 'build-lock', quantity: 5)
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
        timeout(time: 20, unit: "MINUTES")
      }
      steps {
        sh './gradlew --no-watch-fs'
      }
    }

    stage('–– Core lib ––') {
      stages {
        stage('Compile') {
          options {
            timeout(time: 1, unit: "HOURS")
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
              '-PzkHosts=...' +
              '-PsolrHosts=...' +
              '-PsolrUser=solr ' +
              '-PsolrPassword=SolrRocks ' +
              ':atlas-web-core:testClasses'
          }
        }

        stage('Test') {
          options {
            timeout(time: 2, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs -PtestResultsPath=ut :atlas-web-core:test --tests *Test'
            sh './gradlew --no-watch-fs :atlas-web-core:jacocoTestReport'
          }
        }
      }
    }

    stage('–– Web app ––') {
      stages {
        stage('Compile') {
          options {
            timeout(time: 1, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs ' +
              '-Pflyway.url=jdbc:postgresql://localhost:5432/postgres ' +
              '-Pflyway.user=postgres ' +
              '-Pflyway.password=postgres ' +
              '-Pflyway.locations=filesystem:./schemas/flyway/scxa ' +
              '-Pflyway.schemas=scxa ' +
              'flywayMigrate'

            sh './gradlew --no-watch-fs ' +
              '-PdataFilesLocation=/test-data ' +
              '-PexperimentFilesLocation=/test-data/scxa ' +
              '-PexperimentDesignLocation=/root/expdesign-rw ' +
              '-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=scxa ' +
              '-PjdbcUsername=postgres ' +
              '-PjdbcPassword=postgres ' +
              '-PzkHosts=...' +
              '-PsolrHosts=...' +
              '-PsolrUser=solr ' +
              '-PsolrPassword=SolrRocks ' +
              ':app:testClasses'
          }
        }

        stage('Test') {
          options {
            timeout(time: 2, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs -PtestResultsPath=ut :app:test --tests *Test'
            sh './gradlew --no-watch-fs -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
            sh './gradlew --no-watch-fs -PtestResultsPath=e2e :app:test --tests *WIT'
            sh './gradlew --no-watch-fs :app:jacocoTestReport'
          }
        }

        stage('–– Build ––') {
          when {
            anyOf {
              branch 'develop'
              branch 'main'
              branch pattern('release/.*', comparator: 'REGEXP')
            }
          }
          stages {
            stage('Provision Node.js build environment') {
              options {
                timeout(time: 1, unit: "HOURS")
              }
              steps {
                sh 'echo \'APT::Acquire::Retries "10";\' > /etc/apt/apt.conf.d/80-retries'
                sh 'apt update && apt install -y libglu1-mesa gcc'
                sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash'
                sh '. ~/.bashrc && nvm install 14 --lts'
                sh '. ~/.bashrc && npm install -g npm-check-updates'
              }
            }

            stage('Update and build ES bundles') {
              options {
                timeout(time: 1, unit: "HOURS")
              }
              steps {
                sh 'if [ "$BRANCH_NAME" = "develop" ]; then WEBPACK_OPTS=-i; else WEBPACK_OPTS=-ip; fi; ' +
                   '. ~/.bashrc && ./compile-front-end-packages.sh ${WEBPACK_OPTS}'
              }
            }

            stage('Assemble WAR file') {
              options {
                timeout(time: 1, unit: "HOURS")
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
      junit 'app/build/e2e/**/*.xml'

      archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
      archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
      archiveArtifacts artifacts: 'app/src/main/webapp/resources/js-bundles/report.html', fingerprint: true, allowEmptyArchive: true
    }
  }
}
