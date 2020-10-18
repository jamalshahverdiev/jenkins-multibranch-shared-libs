#!/usr/bin/env groovy

class AtlasFrontMethods implements Serializable {
    
    def dsl
    public AtlasFrontMethods( def dsl ) {
       this .dsl = dsl
    }

    String getDeployEnv(branch) {
        if (branch == "develop") {
            return dsl.DEPLOY_ENV_DEV
        }
        else if (branch == "preprod") {
             return dsl.DEPLOY_ENV_PRE
        }
        else {
             def tag = getGitTag()
             if (tag != "no") {
                return dsl.DEPLOY_ENV_PROD
             }
        }
        return dsl.DEPLOY_ENV_DEV
    }

    String getGitTag() {
        def tag = dsl.sh(
                script: '''#!/bin/bash
                     chmod +x ./bash-functions.sh
                     . ./bash-functions.sh
                     identifyGitTag
                     ''',
                returnStdout: true
            ).trim()
        return tag
    }
    
}
