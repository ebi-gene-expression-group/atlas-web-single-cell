#!/usr/bin/env bash
set -euo pipefail

function show_usage {
  echo "Usage: compile-front-end-package.sh [OPTION]..."
  echo "Transpile front end components of (Single Cell) Expression Atlas to Webpack bundles."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-i\tRemove package-lock.json and node_modules directory"
  echo -e "-u\tUpgrade packages of scope @ebi-gene-expression-group to their latest versions (pre-releases such as alpha/beta apply)"
  echo -e "-a\tRun npm audit fix after npm install"
  echo -e "-l\tFast local mode: skip npm install/audit/upgrade steps and reuse existing dependencies"
  echo -e "-p\tGenerate Webpack bundles in production mode"
}

# Prerequistes
if ! which npm >/dev/null 2>&1; then
  echo "npm is not installed. Install Node and try again." && exit 1
fi

WEBPACK_OPTS="--mode development --devtool source-map"
RUN_AUDIT_FIX=false
INIT=false
FAST_LOCAL=false
UPGRADE=false
while getopts ":ailuph" opt; do
  case $opt in
    a)
      RUN_AUDIT_FIX=true
      ;;
    i)
      INIT=true
      ;;
    l)
      FAST_LOCAL=true
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

if [ "$UPGRADE" = true ] && ! which ncu >/dev/null 2>&1; then
  echo "ncu is not installed. Install it with \`npm install -g npm-check-updates\` or run without -u." && exit 1
fi

function update_npm_package {
  if [ "$FAST_LOCAL" = true ]; then
    echo ">> $PWD$ fast local mode: reusing existing node_modules/package-lock.json"
    return
  fi
  # the latest version @ebi-gene-expression-group/eslint-config has conflict eslint version matching with some front-end components
  if [ "$UPGRADE" = true ]; then
      echo ">> $PWD$ ncu --filter /@ebi-gene-expression-group/ --pre 1 --reject @ebi-gene-expression-group/eslint -u "
      ncu --filter '/@ebi-gene-expression-group.*/' --pre 1 --reject @ebi-gene-expression-group/eslint-config -u
    fi
    if [ "$INIT" = true ]; then
      echo ">> $PWD$ rm -rf node_modules package-lock.json"
      rm -rf node_modules package-lock.json
    fi
    echo ">> $PWD$ npm install"
    npm install
    if [ "${RUN_AUDIT_FIX:-false}" = true ]; then
      echo ">> $PWD$ npm audit fix"
      npm audit fix
    fi
}

cd app/src/main/javascript

pushd .
cd modules
for MODULE_DIR in `ls`
do
  pushd .
  cd $MODULE_DIR
  update_npm_package
  # npm install already runs this package's prepare script; run it explicitly only
  # when we skip install (fast local) or when build artifacts are still missing.
  if [ "$FAST_LOCAL" = true ] || [ ! -d lib ]; then
    echo ">> $PWD$ npm run prepare"
    npm run prepare
  fi
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
