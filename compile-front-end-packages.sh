#!/usr/bin/env bash

function show_usage {
  echo "Usage: compile-front-end-package.sh [OPTION]..."
  echo "Transpile front end components of (Single Cell) Expression Atlas to Webpack bundles."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-i\tRemove package-lock.json and node_modules directory"
  echo -e "-u\tUpgrade packages of scope @ebi-gene-expression-group to their latest versions (pre-releases such as alpha/beta apply)"
  echo -e "-p\tGenerate Webpack bundles in production mode"
}

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed. Install Node and then run \`npm install -g npm-check-updates\`." && exit 1
  fi
done

WEBPACK_OPTS="--mode development --devtool source-map"
while getopts ":iuph" opt; do
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
    h)
      show_usage
      exit 0
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      show_usage
      exit 1
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
