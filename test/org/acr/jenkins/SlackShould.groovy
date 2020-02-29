package com.peanutbutter.jenkins

import mocks.WorkflowScriptStub
import spock.lang.Specification

class SlackShould extends Specification {

  class WithSlackScriptStub extends WorkflowScriptStub{
    def count = 0
    def recorder = [:]

    def slackSend(Map args){
      count++
      recorder[count] = args

      println "call count ${count} -> slackSend${args}"
      'DEFAULT_SLACKSEND_RETURN_VALUE'
    }
  }

  def 'pass the right args to send slack method'() {
    given:
    def teamDomain = 'watership'
    def channel = 'fuzzy bunny'
    def message = 'time to dance'
    def buildVersion = '1.2.3'
    def status = 'funky'
    def slackId = 'fake'
    def userEmaill = 'fake@bogas.org'
    def slackAppToken = 'garbage'

    def script = Spy(WithSlackScriptStub)
    def slack = new Slack(script, 'id1', 'id2')

    slack.@_slackAppToken = slackAppToken
    slack.@_slackUserList = [
      members: [
        [
          profile: [
            email: userEmaill
          ],
          id: slackId
        ]
      ]
    ]
    slack.@_culpritEmailAddresses = [userEmaill]

    script.env['BUILD_URL'] = 'https://blah.com'
    
    when:
    slack.sendMessage(teamDomain, channel, status, message, buildVersion)

    then:
    script.count == 2

    and:
    script.recorder[1] == [channel:"#${channel}", color:status, message:"<${script.env['BUILD_URL']}|#${buildVersion}> : ${message} : [<@${slackId}>]", teamDomain:teamDomain, token:slackAppToken]
    script.recorder[2] == [channel:"@${slackId}", color:status, message:"<${script.env['BUILD_URL']}|#${buildVersion}> : ${message}", teamDomain:teamDomain, token:slackAppToken]
  }
}
