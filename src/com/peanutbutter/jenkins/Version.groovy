package com.peanutbutter.jenkins

class Version implements Serializable {

  final def getVersionCommand = 'git rev-list --no-merges --count $(git rev-parse HEAD) -- .'

  private final def script
  private final def versionPrefix

  Version(def script, versionPrefix) {
    this.script = script
    this.versionPrefix = versionPrefix
  }

  def changeDisplayNameToBuildVersion(){
    this.script.currentBuild.displayName = buildVersion
  }

  private def _gitVersion
  private def getGitVersion(){
    if (_gitVersion){
      return _gitVersion
    }

    _gitVersion = this.script.sh(
      script: getVersionCommand,
      returnStdout: true
    ).trim()

    return _gitVersion
  }

  private def _buildVersion
  def getBuildVersion(){
    if (_buildVersion){
      return _buildVersion
    }

    _buildVersion = "${versionPrefix}.${gitVersion}"
    if (this.script.env.BRANCH_NAME != 'master') {
      _buildVersion = _buildVersion + "-${this.script.env.BRANCH_NAME}"
    }

    return _buildVersion
  }
}