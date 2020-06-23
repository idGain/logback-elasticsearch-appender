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

buildArtifact() {
    echo "Branch is ${BRANCH_NAME}"

    if [[ $TRAVIS_BRANCH == "release" ]]; then
        exeinf "Release build"

        exeinf "Performing maven release"
        ./mvnw -B -s .travis/settings.xml release:clean release:prepare release:perform -DscmCommentPrefix="[skip ci] [maven-release-plugin] " -DcheckModificationExcludeList=.travis/*.sh

        pushTagsAndCommit
    else
        exeinf "Travis Snapshot build"
        ./mvnw -s .travis/settings.xml package -Dgpg.skip
    fi
}

main "$@"