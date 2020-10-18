#!/usr/bin/env groovy

class GlobalMethods implements Serializable {
    
    def dsl
    public GlobalMethods( def dsl ) {
       this .dsl = dsl
    }

    public def waitSonarQube(def maxRetry = 3, def timeoutsecs = 10) {
        for (int i = 0; i < maxRetry; i++) {
            try {
                dsl.timeout(time: timeoutsecs, unit: 'SECONDS') {
                    def qg = dsl.waitForQualityGate()
                    if (qg.status != 'OK') {
                        dsl.error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        dsl.waitForQualityGate()
                    }
                }
            }
            catch (Exception e) {
                if (i == maxRetry - 1) { throw e }
            }
        }
    }

    public def waitMsKubernetes(def timeinsecs = 5) {
        dsl.timeout(timeinsecs) {
            dsl.waitUntil {
                dsl.script {
                    def r = dsl.sh(script:"if [ \"\$(kubectl get pod --kubeconfig ${dsl.KUBE_CREDS} -o jsonpath=\"{..ready}\" -l app=${dsl.MS_NAME})\" = \"false\" ]; then exit 1; else exit 0; fi", returnStatus: true)
                    return (r == 0);
                }
            }
        }
        dsl.echo "KUBE_CREDS: ${dsl.KUBE_CREDS}"
        dsl.echo "KUBE_MS_NAME: ${dsl.MS_NAME}"
    }
  
    String currentDeploymentTag(envKubeConfig, deploymentName, kubeNS) {
        dsl.echo String.format("Current deployment TAG: %s, Deployment name: %s, NameSpace: %s.", envKubeConfig,deploymentName,kubeNS)
        def result = dsl.sh(script: "kubectl get --kubeconfig ${envKubeConfig} deployment $deploymentName -n ${kubeNS} -o=jsonpath=\'{\$.spec.template.spec.containers[:1].image}\' | awk -F \':\' \'{ print \$(NF)}\'", 
                            returnStdout: true).trim()
        dsl.echo result
        return result
    }

    String createJiraTicket(projectKeyName, jiraSiteProfile) {
        def testIssue = [fields: [ project: [key: projectKeyName],
        	summary: 'JIRA Ticket Created from Jenkins.',
        	description: 'JIRA Created from Jenkins due the revert issue.',
            issuetype: [id: '10004']]]

        def response = dsl.jiraNewIssue issue: testIssue, site: jiraSiteProfile
        dsl.echo response.successful.toString()
        dsl.echo response.data.toString()
        return response.data.key
    }
  
    String sendToJira(currentDeployedTAG, jiraTicketNumber, jiraSiteProfile) {
        def commitMessage = dsl.sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
        dsl.echo commitMessage
        def commitHash = dsl.sh(returnStdout: true, script: 'git log | head -n1 | awk \'{ print $2 }\'').trim()
        def commitUrlWithoutHash = dsl.sh(
            script: '''#!/bin/bash 
                       linkToRepo=$(git remote -v | head -n1 | awk '{ print $2 }' | sed s/.git//)
					   prjName=$(echo $linkToRepo | cut -f5 -d '/')
					   repName=$(echo $linkToRepo | cut -f6 -d '/')
                       echo $linkToRepo | sed "s|scm/$prjName|projects/$prjName/repos|g; s|$repName|$repName/commits|g"
                       ''',
            returnStdout: true
        ).trim()
        
        def message = String.format("Commit message: %s\n Tag from Kubernetes: %s\n Commit Url: %s", 
                                    commitMessage, currentDeployedTAG, commitUrlWithoutHash + "/" + commitHash)
        dsl.jiraAddComment idOrKey: jiraTicketNumber, comment: message, site: jiraSiteProfile
    }

    void postToSlack(curlCreds, jenkinsUrl, projectName, jobName, jobUrl, kubernetesTag, rolledBackTag) {
        def postToJenkins = dsl.sh(
            script: """
                    curl -s -k -X POST -L --user $curlCreds \"$jenkinsUrl/$projectName/job/$jobName/buildWithParameters?rollbackTag=${rolledBackTag}&kubernetesTag=${kubernetesTag}&buildUrl=${jobUrl}\"
                    """,
            returnStdout: true
        ).trim()
    }   

    void createTicketSendToJira(projectKeyName, currentDeployedTAG, jiraSiteProfile) {
        def ticketNo = createJiraTicket(projectKeyName, jiraSiteProfile)
        def sendToJiraVar = sendToJira(currentDeployedTAG, ticketNo, jiraSiteProfile)
    }  
  
    void compareTagsAndNotifyJiraSlack(jenkinsUrl, jobUrl, jiraSiteProfile) {
      def currentDeployedTAG = currentDeploymentTag(dsl.KUBE_CREDS, dsl.MS_NAME, dsl.PROJECT_NAME)
      dsl.echo "Current Tag: ${dsl.TAG}"
      if (dsl.TAG .startsWith('v') && (cleanTagStrings(dsl.TAG) < cleanTagStrings(currentDeployedTAG))) {
          // Create new Ticket to send result to the Jira ticket system
          createTicketSendToJira(dsl.PROJECT_NAME.toUpperCase(), currentDeployedTAG, jiraSiteProfile)
          postToSlack(dsl.JENKINS_API_CREDS, jenkinsUrl, dsl.PROJECT_NAME, "Test-Slack", jobUrl, currentDeployedTAG, dsl.TAG)
      }
  	}
  
    int cleanTagStrings(String tag) {
        return tag.replace('v', '').replace('.', '').toInteger()
    }
}
