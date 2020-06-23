#!/usr/bin/env bash

set -e

echo "Create release"

gpg2 --keyring=target/pubring.gpg --no-default-keyring --import deploy/signingkey.asc
gpg2 --allow-secret-key-import --keyring=target/secring.gpg --no-default-keyring --import deploy/signingkey.asc
mvn clean deploy --settings deployment/settings.xml -Dgpg.executable=gpg2 -Dgpg.keyname=750883F01279D1B86AB2FD1420E2452361752227 \
  -Dgpg.passphrase=$GPG_PASSWORD -Dgpg.publicKeyring=target/pubring.gpg -Dgpg.secretKeyring=target/secring.gpg





./mvnw initialize release:clean release:prepare release:perform --settings ./travis/settings.xml -DreleaseVersion=$TRAVIS_TAG -DskipTests=true -DcheckModificationExcludeList=travis/*.sh --batch-mode --update-snapshots -Prelease