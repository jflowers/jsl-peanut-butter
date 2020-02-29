package com.peanutbutter.jenkins

import mocks.WorkflowScriptStub
import spock.lang.Specification

class VersionShould extends Specification {

  private Version version
  private def script

  def versionPrefix = '2.7'

  void setup() {
    script = Spy(WorkflowScriptStub)
    version = new Version(script, versionPrefix)
  }

  def 'set build description for master branch'() {
    def gitCommitCount = '32'

    given:
    script.env >> [BRANCH_NAME: 'master']
    script.sh([script: version.getVersionCommand, returnStdout: true]) >> gitCommitCount
    
    when:
    version.changeDisplayNameToBuildVersion()

    then:
    script.currentBuild.displayName == "${versionPrefix}.${gitCommitCount}"
  }
  
  def 'set build description for other branch'() {
    def gitCommitCount = '27'
    def branchName = 'other'

    given:
    script.env >> [BRANCH_NAME: branchName]
    script.sh([script: version.getVersionCommand, returnStdout: true]) >> gitCommitCount
    
    when:
    version.changeDisplayNameToBuildVersion()

    then:
    script.currentBuild.displayName == "${versionPrefix}.${gitCommitCount}-${branchName}"
  }
}
