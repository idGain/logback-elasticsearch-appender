#!/usr/bin/env bash

set -e

echo "Create release"
./mvnw initialize release:clean release:prepare release:perform --settings ./travis/settings.xml -DreleaseVersion=$TRAVIS_TAG -DskipTests=true -DcheckModificationExcludeList=travis/*.sh --batch-mode --update-snapshots -Prelease