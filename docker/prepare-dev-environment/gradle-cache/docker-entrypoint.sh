#!/usr/bin/env bash
./gradlew :atlas-web-core:test
./gradlew :app:test
./gradlew :cli:test
./gradlew :cli:bootRun

printf '\n%b\n\n' "🙈 Ignore any errors above: our only goal is to have Gradle dependencies in the local cache."

rsync -av /root/.gradle/wrapper/dists/* /gradle-wrapper-dists/
rsync -av --exclude=*.lock --exclude=gc.properties /root/.gradle/caches/modules-2 /gradle-ro-dep-cache/
