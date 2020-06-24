#!/usr/bin/env bash

main() {
    init
    setup_git
    buildArtifact
}

init() {
    exeinf "Starting..."
}

# From gist - https://gist.github.com/Bost/54291d824149f0c4157b40329fceb02c
tstp() {
    date +"%Y-%m-%d %H:%M:%S,%3N"
}

# From gist - https://gist.github.com/Bost/54291d824149f0c4157b40329fceb02c
exeinf() {
    echo "INFO " $(tstp) "$ "$@
}

# From gist - https://gist.github.com/Bost/54291d824149f0c4157b40329fceb02c
exeerr() {
    echo "ERROR" $(tstp) "$ "$@
}

setup_git() {
    exeinf "Setting up git"
    git config --global user.email "carsten@cgo-it.de"
    git config --global user.name "Carsten Götzinger"
}

fix_git() {
    git checkout ${TRAVIS_BRANCH}
    git branch -u origin/${TRAVIS_BRANCH}
    git config branch.${TRAVIS_BRANCH}.remote origin
    git config branch.${TRAVIS_BRANCH}.merge refs/heads/${TRAVIS_BRANCH}
}

provide_gpg_keys() {
  rm -rf ${GPG_DIR} && mkdir -p ${GPG_DIR}
  openssl aes-256-cbc -K $encrypted_f3cb6cb238ed_key -iv $encrypted_f3cb6cb238ed_iv -in deploy/signingkey.asc.enc -out ${GPG_DIR}/signingkey.asc -d
  gpg2 --keyring=${GPG_DIR}/pubring.gpg --no-default-keyring --import ${GPG_DIR}/signingkey.asc
  gpg2 --allow-secret-key-import --keyring=${GPG_DIR}/secring.gpg --no-default-keyring  --import ${GPG_DIR}/signingkey.asc

  gpg2 --list-keys
  gpg2 --list-secret-keys
}

get_version() {
  mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -q -DforceStdout
}

delete_tag() {
  local version="$1"
  git push --delete origin v${version}
}

create_pull_request() {
  local version="$1"
  payload=$( cat <<- EOP
        {
          "title": "[skip ci] [maven-release-plugin] Integrate changes from release ${version}",
          "body": "Please pull these changes from release build!",
          "head": "release",
          "base": "master"
        }
EOP
)
  curl -s -H "Authorization: token ${GITHUB_TOKEN}" -H "Accept: application/vnd.github.v3+json" -d "${payload}" https://api.github.com/repos/cgoIT/logback-elasticsearch-appender/pulls
}

on_failure() {
    exeinf "Rollback release"
    mvn -B -s .travis/settings.xml release:rollback -DscmCommentPrefix="[skip ci] [maven-release-plugin] " -DcheckModificationExcludeList=.travis/*.sh -Prelease
}

on_success() {
    exeinf 'Create pull request'

    version="$(get_version)"
    create_pull_request "${version}"
}

buildArtifact() {
    echo "Branch is ${BRANCH_NAME}"

    if [[ "$TRAVIS_BRANCH" == "release" && "$TRAVIS_PULL_REQUEST" == "false" ]]; then
        exeinf "Release build"

        exeinf "Fix git checkout"
        fix_git

        exeinf "Provide gpg keys"
        provide_gpg_keys

        exeinf "Performing maven release"
        mvn -B -s .travis/settings.xml clean initialize release:prepare release:perform -DscmCommentPrefix="[skip ci] [maven-release-plugin] " -DcheckModificationExcludeList=.travis/*.sh -Prelease
        rc="$?"

        if [[ "$rc" -ne 0 ]] ; then
          exeinf 'Release build successful.'
          on_success
        else
          exeerr 'Release build not successful. Rollback...';
          on_failure
          exit $rc
        fi
    else
        exeinf "Travis snapshot build"
        mvn -s .travis/settings.xml package -Dgpg.skip
    fi
}

main "$@"