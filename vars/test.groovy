#!/usr/bin/groovy

//def baseUrl = new URL('http://api.duckduckgo.com')
//def queryString = 'q=groovy&format=json&pretty=1'
//def connection = baseUrl.openConnection()
//connection.with {
//  doOutput = true
//  requestMethod = 'POST'
//  outputStream.withWriter { writer ->
//    writer << queryString
//  }
//  println content.text
//}
def urltoJob = "https://jenkins.kblab.local"
void postToSlack(curlCreds, jenkinsUrl, projectName, jobName, jobUrl) {
//   def postToJenkins = dsl.sh(
//            script: """
//                    curl -s -k -X POST -L --user $curlCreds \"$jenkinsUrl/$projectName/job/$jobName/buildWithParameters?buildUrl=${jobUrl}\"
//                    """,
//            returnStdout: true
//        ).trim()
    def postToJenkins = "curl -s -k -X POST -L --user $curlCreds \"$jenkinsUrl/$projectName/job/$jobName/buildWithParameters?buildUrl=${jobUrl}\""
    println postToJenkins
}
//curl -s -k -X POST -L --user $JENKINS_API_CREDS "https://jenkins.kblab.local/job/Atlas/job/Test-Slack/buildWithParameters?buildUrl=${replaceJksScheme}"
postToSlack("admin:123", "https://jenkins.kblab.local/job", "atlas", "Test-Slack", urltoJob)
