#!/usr/bin/env bash

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed." && exit 1
  fi
done

cd app/src/main/javascript
pushd .

cd modules
for MODULE_DIR in `ls`
do
  pushd .
  cd $MODULE_DIR
  ncu /@ebi-gene-expression-group/ -u
  npm install
  npm audit fix
  npm run prepare
  popd
done

popd
pushd .

cd bundles
for BUNDLE_DIR in `ls`
do
  pushd .
  cd $BUNDLE_DIR
  ncu /@ebi-gene-expression-group/ -u
  npm install
  npm audit fix
  popd
done

popd

WEBPACK_OPTS=${1:-"-d"}

ncu /@ebi-gene-expression-group/ -u
npm install
npm audit fix
npx webpack $WEBPACK_OPTS

