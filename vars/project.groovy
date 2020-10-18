#!/usr/bin/env groovy

String getDeployEnv(branch) {
    if (branch == "develop") {
        return DEPLOY_ENV_DEV
    }
    else if (branch == "preprod") {
         return DEPLOY_ENV_PRE
    }
    else {
         def tag = getGitTag()
         if (tag != "no") {
            return DEPLOY_ENV_PROD
         }
    }
    return branch
}

String getGitTag() {
    def tag = sh(
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
    def isMergeRequest = sh(
        script: '''#!/bin/bash
                   chmod +x ./bash-functions.sh
                   . ./bash-functions.sh
                   identifyMergeRequest "$(git log --oneline -1 --decorate=full)"
                   ''',
        returnStdout: true
    ).trim()
    echo "IS_MERGE_REQUEST func result: ${isMergeRequest}"
    return isMergeRequest
}

void dockerLogin(dockerPassword) {
   echo "Login to the Docker"
   sh "for port in 8083 8089; do docker login -u jenkins -p ${dockerPassword} nexus.kblab.local:\$port/v1/repositories/; done"
   echo "Docker login successful"
}

String generateTag(currentDeployEnv) {
    echo "Generating tag by current env: $currentDeployEnv"
    def tag = "${BUILD_TIMESTAMP}"
    if (currentDeployEnv == DEPLOY_ENV_PROD) {
        tag = "${GIT_BRANCH}"
    }
    echo "Generated tag is: ${tag}"
    return tag
}

void helmMasterToWorkspace(gitCredId, gitURL) {
    git branch: 'master', credentialsId: gitCredId, url: gitURL
}

boolean isNotDeploymentRequest() {
    def isNotDeploymentRequest = true
    if (CURRENT_DEPLOY_ENV == DEPLOY_ENV_PRE || CURRENT_DEPLOY_ENV == DEPLOY_ENV_DEV) {
        isNotDeploymentRequest = IS_MERGE_REQUEST == 'no'
    }else if (CURRENT_DEPLOY_ENV == DEPLOY_ENV_PROD) {
        isNotDeploymentRequest = false
    }
    return isNotDeploymentRequest
}
