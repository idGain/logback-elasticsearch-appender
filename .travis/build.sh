#!/usr/bin/env bash

# Stop on error
set -e

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

pushTagsAndCommit() {
    exeinf "Pushing tags"
    git push --tags
    exeinf "Pushing maven commit"
    git push -u origin release
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

buildArtifact() {
    echo "Branch is ${BRANCH_NAME}"

    if [[ $TRAVIS_BRANCH == "release" ]]; then
        exeinf "Release build"

        exeinf "Fix git checkout"
        fix_git

        exeinf "Provide gpg keys"
        provide_gpg_keys

        exeinf "Performing maven release"
        mvn -B -s .travis/settings.xml clean release:prepare release:perform -DscmCommentPrefix="[skip ci] [maven-release-plugin] " -DcheckModificationExcludeList=.travis/*.sh -Prelease

        pushTagsAndCommit
    else
        exeinf "Travis Snapshot build"
        mvn -s .travis/settings.xml package -Dgpg.skip
    fi
}

main "$@"