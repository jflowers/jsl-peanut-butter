import groovy.transform.Field

jsl = library(
  identifier: "jsl-peanut-butter@${env.BRANCH_NAME}",
  retriever: modernSCM(
    [
      $class: 'GitSCMSource',
      remote: 'https://github.com/jflowers/jsl-peanut-butter.git',
      credentialsId: 'github'
    ]
  )
)

// -----------------------------------------
// the following variables are customizable
// -----------------------------------------
@Field
def appName = 'pipeline'

@Field
def versionPrefix = '1.0'

// -----------------------------------------
//      Do not change the values below
// -----------------------------------------
@Field
def project = 'jsl-peanutbutter'

@Field
def image = "${project}/${appName}"

def getImageTag(){
  return "${image}:${version.buildVersion}"
}

@Field
def LAST_STAGE


version = jsl.com.peanutbutter.jenkins.Version.new(this, versionPrefix)
errorSummary = jsl.com.peanutbutter.jenkins.ErrorSummary.new(this)
slack = jsl.com.peanutbutter.jenkins.Slack.new(this, 'slack-pipeline-token', 'SLACK_LEGACY_TOKEN')


pipeline{
  agent {
    kubernetes {
      label "${appName}-build-pod"
      yamlFile 'jenkins/executor.yaml'
    }
  }

  environment{
    DOCKER_CREDS = credentials('docker-hub')
    DOCKER_AUTH = credentials('docker-auth')

    SONAR_SCANNER = credentials('sonar-scanner')
  }

  stages{
    stage('Checkout'){
      steps{
        script{LAST_STAGE = env.STAGE_NAME}
        checkout scm
        sh 'git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"'
        sh 'git config user.name Jenkins'
        sh 'git config user.email jenkins@bogas.com'

        script{version.changeDisplayNameToBuildVersion()}
      }
    }

    stage('Test'){
      steps{
        script{LAST_STAGE = env.STAGE_NAME}
        container('gradle') {
          sh 'gradle cobertura'
          sh 'gradle coverageCheck'
        }
      }
    }
  }

  post{
    always{
      publishHTML target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'build/reports/cobertura',
        reportFiles: 'index.html',
        reportName: 'Coverage-Report'
      ]

      cobertura coberturaReportFile: "build/reports/cobertura/coverage.xml", onlyStable: false, failNoReports: false, failUnhealthy: false, failUnstable: false

      junit 'build/test-results/test/*.xml'
    }
    success {
      sh "git tag -m '' ${version.buildVersion}"
      withCredentials([
        usernamePassword(credentialsId: 'github', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')
      ]) {
        sh "git push origin ${version.buildVersion}"
      }

      script{slack.notifySuccess('dmd', appName, version.buildVersion)}
    }
    failure {
      script{errorSummary.generate(LAST_STAGE)}

      script{slack.notifyFailure('dmd', appName, version.buildVersion, LAST_STAGE)}
    }
  }
}
