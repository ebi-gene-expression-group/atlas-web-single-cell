# Single Cell Expression Atlas

## Requirements
- Docker v19+
- Docker Compose v1.25+
- 45 GB of available storage for the following Docker volumes:
  - Experiment files
  - Bioentity properties (i.e. gene annotations)
  - PostgreSQL
  - SolrCloud and ZooKeeper
  - Tomcat configuration files

Files managed by either Solr or PostgreSQL are kept in volumes which will be reused even if the containers are removed 
or brought down by Docker Compose. If you want to start afresh delete the old volume (e.g. for Postgres 
`docker volume rm scxa-postgres-11-pgdata`) and re-run the necessary bootstrap script to return to the initial state.
You can find the volume names used by each service in the `volumes` section of its Docker Compose YAML file.

## Code
Clone the repositories of both Atlas Web Core (common business logic for bulk Expression Atlas and Single Cell
Expression Atlas) and Single Cell Expression Atlas proper:
```bash
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-core.git && \
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git
```

If you have already cloned the project ensure it’s up-to-date:
```bash
  git pull
  git submodule update --remote
```

## Create a Gradle read-only dependency cache
To speed up builds and tests it is strongly encouraged to create a Docker volume to back a [Gradle read-only dependency
cache](https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:ephemeral-ci-cache).
```bash
cd docker/prepare-dev/environment/gradle-ro-dep-cache
./run.sh -l gradle-ro-dep-cache.log
```

## Bring up the environment
Besides `ATLAS_DATA_PATH` you need to set some variables for the Postgres container. Use the settings below and replace
`ATLAS_DATA_PATH` value to the directory you set up in the first step.

In the `atlas-web-single-cell/docker` directory run the following:
```bash
ATLAS_DATA_PATH=/path/to/sc/atlas/data \ 
POSTGRES_HOST=scxa-postgres \
POSTGRES_DB=gxpatlasloc \
POSTGRES_USER=atlas3dev \
POSTGRES_PASSWORD=atlas3dev \
docker-compose -f docker-compose-solrcloud.yml -f docker-compose-postgres.yml -f docker-compose-tomcat.yml up
```

You can also set a Docker Compose *Run* configuration in IntelliJ IDEA with the environment variables from the command
above if you find that more convenient.

After bringing up the containers, you may want to inspect the logs to see that all services are running fine. The last
log should come from Tomcat, and it should be something similar to:
```
scxa-tomcat    | 18-Dec-2020 13:40:58.907 INFO [main] org.apache.catalina.startup.Catalina.start Server startup in 6705 ms
```

Now let’s populate both the Postgres database and the SolrCloud collections.

### Postgres
Run the  following command to restore Postgres data from the provided `pg-dump.bin` file:
```bash
docker exec -it scxa-postgres bash -c 'pg_restore -d $POSTGRES_DB -h localhost -p 5432 -U $POSTGRES_USER --clean /var/backups/postgresql/pg-dump.bin'
```

You’ll see some errors due to the way `pg_dump` and `pg_restore` deal with partitioned tables (`scxa_analytics` in our
case). This is normal; they can be safely ignored.

A few minutes later your Postgres database will be ready.

### SolrCloud
Use the provided `Dockerfile` to bootstrap SolrCloud:
```bash
docker build -t scxa-solrcloud-bootstrap .
docker run -i --rm --network scxa scxa-solrcloud-bootstrap
```

You will see many warnings or errors in Solr’s responses. That’s alright and to be expected, since the scripts that
create the config sets, collections and define the schemas will attempt first to remove them to start from a clean,
known state; however Solr will reply with an error if the collections can’t be deleted.

Again, this step will take a few minutes.

### Tomcat
Copy the Tomcat credentials file to the container. The `admin` role is used to access several admin endpoints in Single
Cell Expression Atlas (e.g. `/admin/experiments/help`). Tomcat’s `conf` directory is persisted as a volume so that we
need to do this only once:
```bash
docker cp tomcat-users.xml scxa-tomcat:/usr/local/tomcat/conf
```

Check your Tomcat logs and look for a line similar to the one below to make sure the new user settings have been
loaded:
```
scxa-tomcat    | 12-Jan-2021 14:58:51.969 INFO [Catalina-utility-2] org.apache.catalina.users.MemoryUserDatabase.backgroundProcess Reloading memory user database [UserDatabase] from updated source [file:/usr/local/tomcat/conf/tomcat-users.xml]
```

Run the Gradle task `war` in the `atlas-web-single-cell` directory:
```bash
cd atlas-web-single-cell
./gradlew -PbuildProfile=docker war
```

You should now have the file `build/libs/gxa#sc.war` which by default Tomcat’s naming conventions will be served at
`gxa/sc`. Tomcat should automatically load the application after a few seconds. You should be seeing something like
this in your logs:
```
scxa-tomcat    | 12-Jan-2021 14:59:47.566 INFO [Catalina-utility-1] org.apache.catalina.startup.HostConfig.deployWAR Deployment of web application archive [/usr/local/tomcat/webapps/gxa#sc.war] has finished in [5,510] ms
```

Point your browser at `http://localhost:8080/gxa/sc` and voilà!

Every time you re-run the `war` task the web app will be automatically re-deployed by Tomcat.

If you get any `war` redeploy issues or want to start again freshly, stop all the containers using this:

```bash
ATLAS_DATA_PATH=/path/to/sc/atlas/data \
POSTGRES_HOST=scxa-postgres \
POSTGRES_DB=gxpatlasloc \
POSTGRES_USER=atlas3dev \
POSTGRES_PASSWORD=atlas3dev \
docker-compose -f docker-compose-solrcloud.yml -f docker-compose-postgres.yml -f docker-compose-tomcat.yml down
```
If you get any `java.net.UnknownHostException: Invalid hostname for server: local` exceptions while running application.
Go to `profile-docker. gradle` file and check these attribute values `ext.zkHost` & `ext.solrHost` and compare with running container names.
If they are different, update them.These should be ZooKeeper and Solr container names.

## Backing up your data
Eventually you’ll add new experiments to your development instance of SCXA, or new, improved collections in Solr will
replace the old ones. In such cases you’ll want to get a snapshot of the data to share with the team. Below there are
instructions to do that.

### PostgreSQL
If at some point you wish to create a backup dump of the database run the command below:
```bash
docker exec -it scxa-postgres bash -c 'pg_dump -d $POSTGRES_DB -h localhost -p 5432 -U $POSTGRES_USER -f /var/lib/postgresql/scxa/pg-dump.bin -F c -n $POSTGRES_USER -t $POSTGRES_USER.* -T *flyway*'
```

### SolrCloud
```bash
for SOLR_COLLECTION in $SOLR_COLLECTIONS
do
  START_DATE_IN_SECS=`date +%s`
  curl "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=backup&location=/var/backups/solr&name=${SOLR_COLLECTION}"

  # Pattern enclosed in (?<=) is zero-width look-behind and (?=) is zero-width look-ahead, we match everything in between
  COMPLETED_DATE=`curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details" | grep -oP '(?<="snapshotCompletedAt",").*(?=")'`
  COMPLETED_DATE_IN_SECS=`date +%s -d "${COMPLETED_DATE}"`

  # We wait until snapshotCompletedAt is later than the date we took before issuing the backup operation
  while [ ${COMPLETED_DATE_IN_SECS} -lt ${START_DATE_IN_SECS} ]
  do
    sleep 1s
    COMPLETED_DATE=`curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details" | grep -oP '(?<="snapshotCompletedAt",").*(?=")'`
    COMPLETED_DATE_IN_SECS=`date +%s -d "${COMPLETED_DATE}"`
  done
done
```

### Update test data
Remember to update the file and any new experiments added to the `filesystem` directory by syncing your
`ATLAS_DATA_PATH` with `/nfs/ftp/pub/databases/microarray/data/atlas/test/scxa`:
```bash
rsync -ravz $ATLAS_DATA_PATH/* ebi-cli:/nfs/ftp/pub/databases/microarray/data/atlas/test/scxa/
```

## Testing
**Note: A few tests depend on the Solr suggesters, so don’t forget to build them in the SolrCloud container!**

The project has a `docker-compose-gradle.yml` in the `docker` directory to run tests within a Gradle Docker container.
It reuses the same SolrCloud service described earlier, and a Postgres container with minor variations: it doesn’t use 
volumes to ensure the database is clean before running any tests, and its name (and the dependency expressed in 
`docker-compose-gradle.yml`) has been changed to `scxa-postgres-test`; the reason is to avoid using `scxa-postgres` by
mistake and wiping full tables when cleaning fixtures... such an unfortunate accident is known to have happened.

Depending on your OS and Docker settings you might be able to run Gradle from your host machine without a container,
and access Solr, ZooKeeper and Postgres via mapped ports on `localhost`. We know this is possible in Linux, but we’ve 
found it to be problematic in macOS. It’s probably due to the way DNS in Docker Compose works (i.e., ZooKeeper resolves 
`localhost` to an unknown IP address). As they say, networking is hard. YMMV. 

### Before you start
Check with `docker ps` and `docker container ls -a` that no services used during tests are running or stopped,
respectively. These are `scxa-solrcloud-1`, `scxa-solrcloud-2`, `scxa-zk-1`, `scxa-zk-2`, `scxa-zk-3`, 
`scxa-postgres-test`, `scxa-flyway-test` and `scxa-gradle`. We want to start with a clean application context every 
time we execute the test task. Here are two useful commands:
```bash
docker stop scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test scxa-gradle
```

```bash
docker rm scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test scxa-gradle
```

### Running tests
As mentioned before, `docker-compose-gradle.yml` runs the Gradle `test` task and it depends on all the necessary 
services to run unit tests, integration tests and end-to-end tests. It splits the job in the following six phases:
1. Clean the build directory
2. Compile the test classes
3. Run unit tests
4. Run integration tests
5. Run end-to-end tests
6. Generate JaCoCo reports

Bring it up like this (the Postgres variables can take any values, remember that the container will be removed):
```bash
ATLAS_DATA_PATH=/path/to/your/scxa/data \
POSTGRES_HOST=scxa-postgres-test \
POSTGRES_DB=gxpscxatest \
POSTGRES_USER=scxa \
POSTGRES_PASSWORD=scxa \
docker-compose \
-f docker-compose-postgres-test.yml \
-f docker-compose-solrcloud.yml \
-f docker-compose-gradle.yml \
up
```

You will eventually see these log messages:
```
scxa-gradle         | BUILD SUCCESSFUL in 2s
scxa-gradle         | 3 actionable tasks: 1 executed, 2 up-to-date
scxa-gradle exited with code 0
```

Press Ctrl+C to stop the container and clean any leftovers:
```bash
docker stop scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test &&
docker rm scxa-solrcloud-1 scxa-solrcloud-2 scxa-zk-1 scxa-zk-2 scxa-zk-3 scxa-postgres-test scxa-flyway-test scxa-gradle
```

If you prefer, here’s a `docker-compose run` command to execute the tests:
```bash
ATLAS_DATA_PATH=/path/to/your/scxa/data \
POSTGRES_HOST=scxa-postgres-test \
POSTGRES_DB=gxpscxatest \
POSTGRES_USER=scxa \
POSTGRES_PASSWORD=scxa \
docker-compose \
-f docker-compose-postgres-test.yml \
-f docker-compose-solrcloud.yml \
-f docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c '
./gradlew :app:clean &&
./gradlew -PdataFilesLocation=/root/scxa/integration-test-data -PexperimentFilesLocation=/root/scxa/integration-test-data/scxa -PjdbcUrl=jdbc:postgresql://$POSTGRES_HOST:5432/$POSTGRES_DB -PjdbcUsername=$POSTGRES_USER -PjdbcPassword=$POSTGRES_PASSWORD -PzkHost=scxa-zk-1 -PsolrHost=scxa-solrcloud-1 app:testClasses &&
./gradlew -PtestResultsPath=ut :app:test --tests *Test &&
./gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT &&
./gradlew -PtestResultsPath=e2e :app:test --tests *WIT &&
./gradlew :app:jacocoTestReport
'
``` 

With `run` the control returns to your shell once the tasks have finished, but you’ll need to clean up the service
containers anyway.

In either case you may find all reports at `app/build/reports`.

### Running a single test
Many times you will find yourself working in a specific test case or class. Running all tests in such cases is
impractical. In such situations you can use 
[Gradle’s continuous build execution](https://blog.gradle.org/introducing-continuous-build). See the example below for
e.g. `ExperimentFileLocationServiceIT.java`:
```bash
ATLAS_DATA_PATH=/path/to/your/scxa/data \
POSTGRES_HOST=scxa-postgres-test \
POSTGRES_DB=gxpscxatest \
POSTGRES_USER=scxa \
POSTGRES_PASSWORD=scxa \
docker-compose \
-f docker-compose-postgres-test.yml \
-f docker-compose-solrcloud.yml \
-f docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c '
./gradlew :app:clean &&
./gradlew -PdataFilesLocation=/atlas-data -PexperimentFilesLocation=/atlas-data/scxa -PjdbcUrl=jdbc:postgresql://$POSTGRES_HOST:5432/$POSTGRES_DB -PjdbcUsername=$POSTGRES_USER -PjdbcPassword=$POSTGRES_PASSWORD -PzkHost=scxa-zk-1 -PsolrHost=scxa-solrcloud-1 app:testClasses &&
./gradlew --continuous :app:test --tests ExperimentFileLocationServiceIT
'
```

After running the test Gradle stays idle and waits for any changes in the code. When it detects that the files in your
project have been updated it will recompile them and run the tests again. Notice that you can specify multiple test
files after `--tests` (by name or with wildcards).

### Remote debugging
If you want to use a debugger, add the option `-PremoteDebug` to the task test line. For instance:
```bash
./gradlew -PremoteDebug :app:test --tests CellPlotDaoIT
```

Be aware that Gradle won’t execute the tests until you attach a remote debugger to port 5005. It will notify you when
it’s ready with the following message:
```
> Task :app:test
Listening for transport dt_socket at address: 5005
<===========--> 90% EXECUTING [5s]
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> :app:test > 0 tests completed
> IDLE
> IDLE
> IDLE
> IDLE
```

You can combine `--continuous` with `-PremoteDebug`, but the debugger will be disconnected at the end of the test. You
will need to start and attach the remote debugger every time Gradle compiles and runs the specified test.

## Troubleshooting

### The script that backs up Solr snapshot hangs
Ensure you have writing privileges for the directory bind at `/var/backups/solr`. You can check the status of your
backup operation with (set `SOLR_HOST` and `SOLR_COLLECTION` to the appropriate values):
```bash
docker exec -i ${SOLR_HOST} curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details"
```

### I’m not getting any suggestions in Single Cell Expression Atlas (tests in `uk.ac.ebi.atlas.search.suggester` fail)
Read the important message after you run `scxa-solrlcoud-bootstrap`:
> PLEASE READ!
> Suggesters haven’t been built because it’s very likely to get a `java.net.SocketTimeoutException` due
> to the size of the bioentities collection. Raising the timeout in Jetty could mask other errors down
> the line, and ignoring the exception doesn’t guarantee the suggester to be fully built since it still
> takes a few extra minutes: the exception is thrown before the process has completed.
> The best option is to manually build and supervise this step.
>
> Run the following commands(don’t worry if the request returns a 500 error after a while):
> 
> `docker exec -i scxa-solrcloud-1 curl 'http://localhost:8983/solr/bioentities-v1/suggest?suggest.build=true&suggest.dictionary=bioentitySuggester&suggest.dictionary=propertySuggesterNoHighlight'`
>
> While the command above is running, monitor the size of the suggester directory size:
> 
> `docker exec -it scxa-solrcloud-1 bash -c 'watch du -sc server/solr/bioentities-v1*/data/*'`
> `docker exec -it scxa-solrcloud-2 bash -c 'watch du -sc server/solr/bioentities-v1*/data/*'`
>
> Both suggesters in every shard will be built when the suggester directory size stabilises.
