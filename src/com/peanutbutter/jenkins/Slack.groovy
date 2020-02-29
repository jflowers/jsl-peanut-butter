package com.peanutbutter.jenkins

import hudson.tasks.Mailer
import hudson.model.User
import groovy.json.JsonSlurperClassic
import com.cloudbees.plugins.credentials.CredentialsProvider

class Slack implements Serializable {

  private final def script
  def slackAppTokenId
  def slackLegacyTokenID

  Slack(script, slackAppTokenId, slackLegacyTokenID) {
    this.script = script
    this.slackAppTokenId = slackAppTokenId
    this.slackLegacyTokenID = slackLegacyTokenID
  }

  def notifySuccess(teamDomain, channel, appName, buildVersion){
    sendMessage(teamDomain, channel, 'good', "${appName} build successful - ${buildVersion}", buildVersion)
  }

  def notifyFailure(teamDomain, channel, appName, buildVersion, lastStage){
    sendMessage(teamDomain, channel, 'danger', "${appName} build failed in stage ${lastStage} - ${buildVersion}", buildVersion)
  }

  def sendMessage(teamDomain, channel, status, message, buildVersion) {
    def build_link = slackifyLink(this.script.env.BUILD_URL, "#${buildVersion}")
    this.script.slackSend(
      channel: "#$channel",
      color: status,
      message: "$build_link : $message : ${addNotifications()}",
      teamDomain: teamDomain,
      token: slackAppToken
    )
    
    culprits.each{culrpit ->
      this.script.slackSend(
        channel: "@${culrpit['id']}",
        color: status,
        message: "$build_link : $message",
        teamDomain: teamDomain,
        token: slackAppToken
      )
    }
  }

  private def findSecret(secretId){
    def jenkinsCredentials = CredentialsProvider.lookupCredentials(
      com.cloudbees.plugins.credentials.Credentials.class,
      Jenkins.instance,
      null,
      null
    );


    def credentials = jenkinsCredentials.find{it.id == secretId}
    
    return credentials.secret.toString()
  }

  private _slackAppToken

  private def getSlackAppToken(){
    if (_slackAppToken){
      return _slackAppToken
    }

    _slackAppToken = findSecret(slackAppTokenId)

    return _slackAppToken
  }

  private _slackLegacyToken

  private def getSlackLegacyToken(){
    if (_slackLegacyToken){
      return _slackLegacyToken
    }

    _slackLegacyToken = findSecret(slackLegacyTokenID)
    
    return _slackLegacyToken
  }

  private _slackUserList

  private def getSlackUserList(){
    if (_slackUserList){
      return _slackUserList
    }

    def connection

    try{
      connection = new URL("https://slack.com/api/users.list?token=${slackLegacyToken}&pretty=1").openConnection()

      def body = connection.inputStream.text
      if (connection.responseCode == 200) {
        _slackUserList = new JsonSlurperClassic().parseText(body)
      } else {
        this.script.echo 'error calling to slack api(${connection.responseCode}):'
        this.script.echo body
        return []
      }

      if (!_slackUserList.members){
        this.script.echo 'error calling to slack api(${connection.responseCode}):'
        this.script.echo body
        return []
      }

      return _slackUserList
    }finally{
      connection.disconnect()
    }
  }

  private def slackIds(emailAddresses){
    slackUserList.members.findResults{member ->
      if (member.profile.email in emailAddresses){
        return [id:member.id, email:member.profile.email]
      }
    }
  }


  private _culpritEmailAddresses

  private getCulpritEmailAddresses(){
    if (_culpritEmailAddresses){
      return _culpritEmailAddresses
    }

    _culpritEmailAddresses = []

    this.script.currentBuild.changeSets.each{ changeSet ->
      changeSet.items.each(){ entry ->
        def email = entry.author.getProperty(Mailer.UserProperty.class).getAddress()

        if (!_culpritEmailAddresses.contains(email)) {
          _culpritEmailAddresses << email
        }
      }
    }

    if (this.script.currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')){
      def userIds = this.script.currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userId
      
      userIds.each(){userId ->
        User user = User.get(userId)
        def email = user.getProperty(Mailer.UserProperty.class).getAddress()
        
        if (!_culpritEmailAddresses.contains(email)) {
          _culpritEmailAddresses << email
        }
      }
    }

    return _culpritEmailAddresses
  }

  private def _culprits

  private def getCulprits(){
    if (_culprits){
      return _culprits
    }

    _culprits = slackIds(culpritEmailAddresses)
    return _culprits
  }

  private def slackifyLink(link, text) {
    return "<$link|$text>"
  }

  private def addNotifications(){
    culprits.inject([]){result, culrpit ->
      result << "<@${culrpit['id']}>"
    }
  }
}
