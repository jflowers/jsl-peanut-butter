package com.peanutbutter.jenkins

class ErrorSummary implements Serializable {

  private final def script
  def errorRegex = /(?i).*(fail|error\s).*/

  ErrorSummary(def script) {
    this.script = script
  }

  def generate(lastStage){
    def log = script.manager.build.logFile.readLines()
    def errorLineNums = log.findIndexValues{ it ==~ errorRegex && !it.contains('skipped due to earlier failure') }
    
    def summary = script.manager.createSummary("error.gif")
    summary.appendText("<h2>Failed ${lastStage} Stage</h2>", false)

    def printed = []
    errorLineNums.each{ errorLineNum ->
      (errorLineNum..errorLineNum+5).each{ lineNum ->
        if (printed.contains(lineNum)){
          return
        }
        def line = log[lineNum.intValue()]
        if (line ==~ /.*\[8mha:\/.*/){
          line = line.split(/\[0m/)[1]
        }
        
        if (!printed.isEmpty() && !printed.contains(lineNum-1)){
          summary.appendText("<br/>", false)
        }
        
        printed << lineNum
        summary.appendText("<p style=\"box-sizing: border-box; line-height: 1.4em; -webkit-font-smoothing: antialiased; font-size: 14px; color: #333; margin: 0; padding: 0; white-space: nowrap; font-family: monospace;\"><a href=\"console#L${lineNum}\">${lineNum}</a><span> $line</span></p>", false)
      }
    }
  }
}