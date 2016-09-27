#!/bin/bash

CURBRANCH=`git rev-parse --abbrev-ref HEAD`
[ "$CURBRANCH" != "master" ] && echo "Can only release from master" && exit 1

echo "Releasing master"
mvn clean release:prepare -Dmaven.test.skip=true --batch-mode -DautoVersionSubmodules=true -Dresume=false || exit 1

echo "Pushing to Stash"
git remote add stash ssh://stash.mnemonic.no:8022/joss/commons
git fetch stash master  || exit 1
git push stash master --tags  || exit 1

echo "Performing release"
mvn release:perform -Dmaven.test.skip=true --batch-mode -Darguments=-DskipTests || exit 1

