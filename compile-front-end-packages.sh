#!/usr/bin/env bash

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed. Install Node and then run \`npm install -g npm-check-updates\`." && exit 1
  fi
done

WEBPACK_OPTS="--mode development --devtool source-map"
while getopts ":iud" opt; do
  case $opt in
    i)
      INIT=true
      ;;
    u)
      UPGRADE=true
      ;;
    p)
      WEBPACK_OPTS="--mode production"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
  esac
done

function update_npm_package {
  if [ "$UPGRADE" = true ]; then
      echo ">> $PWD$ ncu /@ebi-gene-expression-group/ --pre 1 -u"
      ncu /@ebi-gene-expression-group/ --pre 1 -u
    fi
    if [ "$INIT" = true ]; then
      echo ">> $PWD$ rm -rf node_modules package-lock.json"
      rm -rf node_modules package-lock.json
    fi
    echo ">> $PWD$ npm install"
    npm install
    echo ">> $PWD$ npm audit fix"
    npm audit fix
}

cd app/src/main/javascript

pushd .
cd modules
for MODULE_DIR in `ls`
do
  pushd .
  cd $MODULE_DIR
  update_npm_package
  echo ">> $PWD$ npm run prepare"
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
  update_npm_package
  popd
done
popd

update_npm_package
echo ">> $PWD$ npx webpack $WEBPACK_OPTS"
npx webpack $WEBPACK_OPTS
