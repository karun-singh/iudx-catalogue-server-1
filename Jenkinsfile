properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/cat-dev'
    deplRegistry = 'ghcr.io/datakaveri/cat-prod'
    testRegistry = 'ghcr.io/datakaveri/cat-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {

    stage('Building images') {
      steps{
        script {
          echo 'Pulled me - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/prod.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Run Unit Tests and CodeCoverage test'){
      steps{
        script{
          sh 'docker-compose up test'
        }
      }
    }

    stage('Capture Unit Test results'){
      steps{
        xunit (
          thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '40') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/TEST-iudx.catalogue.server.apiserver*.xml') ]
        )
      }
      post{
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }

    stage('Capture Code Coverage'){
      steps{
        jacoco classPattern: 'target/classes', execPattern: 'target/DatabaseServiceTest.exec,target/jacoco2.exec', sourcePattern: 'src/main/java', sourceExclusionPattern: 'iudx/catalogue/server/apiserver/*,iudx/catalogue/server/deploy/*,iudx/catalogue/server/mockauthenticator/*,iudx/catalogue/server/apiserver/util/*,iudx/catalogue/server/**/*EBProxy.*,iudx/catalogue/server/**/*ProxyHandler.*,iudx/catalogue/server/**/reactivex/*,iudx/catalogue/server/**/reactivex/*'
      }
    }

    stage('Run Cat server for Performance Tests'){
      steps{
        script{
            sh 'scp Jmeter/CatalogueServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Jmeter/'
            sh 'scp src/test/resources/iudx-catalogue-server.postman_collection_test.json jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Newman/'
            sh 'docker-compose up -d perfTest'
            sh 'sleep 45'
        }
      }
    }
    
    stage('Run Jmeter Performance Tests'){
      steps{
        node('master') {      
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/cat/Jmeter/Report ; mkdir -p /var/lib/jenkins/iudx/cat/Jmeter/Report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/cat/Jmeter/CatalogueServer.jmx -l /var/lib/jenkins/iudx/cat/Jmeter/Report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/cat/Jmeter/Report'
          }
        }
      }
    }
    
    stage('Capture Jmeter report'){
      steps{
        node('master') {
          perfReport errorFailedThreshold: 0, errorUnstableThreshold: 0, filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/cat/Jmeter/Report/*.jtl'
        }
      }
      post{
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }

    stage('OWASP ZAP pen test'){
      steps{
        node('master') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
              sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/cat/Newman/iudx-catalogue-server.postman_collection_test.json -e /home/ubuntu/configs/cat-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/cat/Newman/report/report.html'
            }
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('master') {
            script{
               archiveZap failAllAlerts: 15
               publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/iudx/cat/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: ''])
            }  
          }
        }
      }
    }
    
    stage('Clean up'){
      steps{
        sh 'docker-compose down --remove-orphans'
      }
    }

    stage('Push Image') {
      when{
        expression {
          return env.GIT_BRANCH == 'origin/master';
        }
      }
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("3.0-${env.GIT_HASH}")
            deplImage.push("3.0-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}
