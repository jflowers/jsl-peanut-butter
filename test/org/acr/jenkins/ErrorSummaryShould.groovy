package com.peanutbutter.jenkins

import mocks.WorkflowScriptStub
import spock.lang.Specification

class ErrorSummaryShould extends Specification {

  class LogFileStub{
    def lines = []
    def readLines(){
      return lines
    }
  }

  class SummaryStub{
    def count = 0
    def recorder = [:]

    def appendText(text, escapeHtml){
      count++
      recorder[count] = [text:text, escapeHtml:escapeHtml]

      println "call count ${count} -> SummaryStub.appendText${recorder[count]}"
    }
  }

  class ManagerStub{
    def build = [logFile: new LogFileStub()]
    
    def createSummaryCount = 0
    def createSummaryRecorder = [:]
    def createSummary(icon){
      def summary = new SummaryStub()

      def args = [icon:icon]

      createSummaryCount++
      createSummaryRecorder[createSummaryCount] = [args:args, summary:summary]

      println "call count ${createSummaryCount} -> ManagerStub.createSummary${args}"
      
      return summary
    }
  }

  def 'generate error summary'() {
    given:
    def lastStage = 'ultimate'

    def script = Spy(WorkflowScriptStub)

    script.manager = new ManagerStub()
    script.manager.build.logFile.lines = [
      'one',
      'two',
      'error something bad',
      'wtih',
      'a',
      'bunch more',
      'lines',
      'afterwards',
      'but not',
      'this much'
    ]


    def errorSummary = new ErrorSummary(script)
    
    when:
    errorSummary.generate(lastStage)

    then:
    script.manager.createSummaryCount == 1

    and: 
    script.manager.createSummaryRecorder[1]['args'] == [icon:'error.gif']

    and:
    script.manager.createSummaryRecorder[1]['summary'].count == 7

    and:
    script.manager.createSummaryRecorder[1]['summary'].recorder[1]['text'].contains(lastStage)

    and:
    script.manager.createSummaryRecorder[1]['summary'].recorder[2]['text'].contains('error something bad')

    and:
    script.manager.createSummaryRecorder[1]['summary'].recorder[7]['text'].contains('afterwards')
  }
}
