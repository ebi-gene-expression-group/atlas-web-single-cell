#!/usr/bin/env bash

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed. Install Node and then run \`npm install -g npm-check-updates\`." && exit 1
  fi
done

cd app/src/main/javascript
pushd .

cd modules
for MODULE_DIR in `ls`
do
  pushd .
  cd $MODULE_DIR
  #ncu /@ebi-gene-expression-group/ -u
  #rm -rf node_modules package-lock.json
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
  #ncu /@ebi-gene-expression-group/ -u
  #rm -rf node_modules package-lock.json
  npm install
  npm audit fix
  popd
done

popd

WEBPACK_OPTS=${1:-"--mode development --devtool source-map"}

ncu /@ebi-gene-expression-group/ -u
#rm -rf node_modules package-lock.json
npm install
npm audit fix
npx webpack $WEBPACK_OPTS

