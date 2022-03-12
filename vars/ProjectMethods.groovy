#!/usr/bin/env groovy

class AtlasMethods implements Serializable {

    def dsl
    public AtlasMethods( def dsl ) {
        this .dsl = dsl
    }

    String getDeployEnv(branch) {
        if (branch == 'develop') {
            return dsl.DEPLOY_ENV_DEV
        }
        else if (branch == 'preprod') {
            return dsl.DEPLOY_ENV_PRE
        }
        else {
            def tag = getGitTag()
            if (tag != 'no') {
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

    String isMergeRequest() {
        def isMergeRequest = dsl.sh(
            script: '''#!/bin/bash
                       chmod +x ./bash-functions.sh
                       . ./bash-functions.sh
                       identifyMergeRequest "$(git log --oneline -1 --decorate=full)"
                       ''',
            returnStdout: true
        ).trim()
        dsl.echo "IS_MERGE_REQUEST func result: ${isMergeRequest}"
        return isMergeRequest
    }

    void dockerLogin(dockerPassword) {
        dsl.echo 'Login to the Docker'
        dsl.sh "for port in 8083 8089; do docker login -u jenkins -p ${dockerPassword} nexus.domain.name:\$port/v1/repositories/; done"
        dsl.echo 'Docker login successful'
    }

    String generateTag(currentDeployEnv) {
        dsl.echo "Generating tag by current env: $currentDeployEnv"
        def tag = "${dsl.BUILD_TIMESTAMP}"
        if (currentDeployEnv == dsl.DEPLOY_ENV_PROD) {
            tag = "${dsl.GIT_BRANCH}"
        }
        dsl.echo "Generated tag is: ${tag}"
        return tag
    }

    void helmMasterToWorkspace(gitCredId, gitURL) {
        dsl.git branch: 'master', credentialsId: gitCredId, url: gitURL
    }

    boolean isNotDeploymentRequest() {
        def isNotDeploymentRequest = true
        if (dsl.CURRENT_DEPLOY_ENV == dsl.DEPLOY_ENV_PRE || dsl.CURRENT_DEPLOY_ENV == dsl.DEPLOY_ENV_DEV) {
            isNotDeploymentRequest = dsl.IS_MERGE_REQUEST == 'no'
        }else if (dsl.CURRENT_DEPLOY_ENV == dsl.DEPLOY_ENV_PROD) {
            isNotDeploymentRequest = false
        }
        return isNotDeploymentRequest
    }

}
