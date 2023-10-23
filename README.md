# Single Cell Expression Atlas

## Prepare your development environment

### TL;DR
```bash
./docker/prepare-dev-environment/gradle-cache/run.sh -r -l gradle-cache.log && \
./docker/prepare-dev-environment/volumes/run.sh -r -l volumes.log && \
./docker/prepare-dev-environment/postgres/run.sh -r -l pg-anndata.log && \
./docker/prepare-dev-environment/postgres/run.sh -r -a -l pg-no-anndata.log && \
./docker/prepare-dev-environment/solr/run.sh -r -l solr.log &&
./build-and-deploy-webapp.sh
```

Point your browser at `http://localhost:8080/gxa/sc` and voilà!

### Requirements
- Docker v20+ with the [Compose plugin](https://docs.docker.com/compose/install/)
- 60 GB of available storage for the following Docker volumes:
  - Experiment files
  - Bioentity properties (i.e. gene annotations)
  - PostgreSQL
  - SolrCloud and ZooKeeper
  - Tomcat configuration files

Files written by Solr, PostgreSQL and Tomcat are kept in volumes which will be reused even if the containers are 
removed (e.g. when running `docker compose down`).  If you want to start afresh each script can remove all volumes 
if you add the `-r` option . You can find the volume names used by each service in the `volumes` section of its Docker 
Compose YAML file.

The full list of volumes is:
- `scxa_gradle-wrapper-dists`
- `scxa_gradle-ro-dep-cache`
- `scxa_atlas-data-bioentity-properties`
- `scxa_atlas-data-exp`
- `scxa_atlas-data-scxa-expdesign`
- `scxa_postgres-11-pgdata-18`
- `scxa_postgres-11-pgdata-latest`
- `scxa_solrcloud-0-data-solr-8.7`
- `scxa_solrcloud-1-data-solr-8.7`
- `scxa_solrcloud-zookeeper-0-data-solr-8.7`
- `scxa_solrcloud-zookeeper-0-datalog-solr-8.7`
- `scxa_solrcloud-zookeeper-1-data-solr-8.7`
- `scxa_solrcloud-zookeeper-1-datalog-solr-8.7`
- `scxa_solrcloud-zookeeper-2-data-solr-8.7`
- `scxa_solrcloud-zookeeper-2-datalog-solr-8.7`
- `scxa_tomcat-conf`

### Code
Clone this repository with submodules:
```bash
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git
```

If you have already cloned the project ensure it’s up-to-date:
```bash
git pull
git submodule update --remote
```

### Create a Gradle read-only dependency cache
To speed up builds and tests it is strongly encouraged to create a Docker volume to back a [Gradle read-only dependency
cache](https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:ephemeral-ci-cache).
```bash
./docker/prepare-dev-environment/gradle-cache/run.sh -r -l gradle-cache.log
```

### Prepare volumes
In order to run integration tests and a development instance of Single Cell Expression Atlas you will need a few Docker
Compose volumes first. They will be populated with data that will be indexed in Solr and Postgres. Single Cell 
Expression Atlas needs all three of: file bundles in the volumes, Solr collections and Postgres data. This step takes 
care of the first requirement:
```bash
./docker/prepare-dev-environment/volumes/run.sh -r -l volumes.log
```

You can get detailed information about which volumes are created if you run the script with the `-h` flag.

This script, unless it’s run with the `-r` flag, can be interrupted without losing any data. The container mirrors 
directories via FTP, and can resume after cancellation. It can be re-run to update the data in the volumes should the 
contents of the source directories change. This is especially useful when experiments are re-analysed/re-annotated, 
or the bioentity properties directory is updated after a release of  Ensembl, WormBase ParaSite, Reactome, Gene 
Ontoloy, Plant Ontology or InterPro. 

### PostgreSQL
To enable easy switching between *anndata* support and earlier versions of the database, the script sets the 
environment variable `SCHEMA_VERSION` to either `latest` or `18` 
([latest version before anndata was 
introduced](https://github.com/ebi-gene-expression-group/db-scxa/commit/1236753d3d799effa4d24fa9bdfb9292c66309ab)),
respectively.  The value is appended to the volume name mounted by the Postgres service.

Run the script twice and then choose later the appropriate version:
```bash
./docker/prepare-dev-environment/postgres/run.sh -r -l pg-anndata.log       # anndata support
./docker/prepare-dev-environment/postgres/run.sh -a -r -l pg-no-anndata.log  # no anndata support
```

To run the Postgres service **with support for anndata experiments**:
```bash
SCHEMA_VERSION=latest \
docker compose --env-file ./docker/dev.env \
-f ./docker/docker-compose-postgres.yml \
up
```

To run the Postgres service **without support for anndata experiments**:
```bash
SCHEMA_VERSION=18 \
docker compose --env-file ./docker/dev.env \
-f ./docker/docker-compose-postgres.yml \
up
```

### Solr
The Solr script can optionally be given a location for the Single Cell Expression Atlas ontology OWL file. Otherwise,
it will download [the published `scatlas.owl` file in the EBI SPOT 
repository](https://github.com/EBISPOT/scatlas_ontology/blob/master/scatlas.owl). It will also generate an RSA keypair
[to sign and verify Solr packages](https://solr.apache.org/guide/8_7/package-manager-internals.html) that you can keep
for reference and to sign other packages (or later versions of BioSolr). Be aware that Solr can store multiple keys, so
they are not strictly necessary; it is possible to generate a new keypair and store its public key every time you add a 
package. Run the script with the `-h` flag for more details.

```bash
./docker/prepare-dev-environment/solr/run.sh -r -l solr.log
```

You may want to speed up the process by raising the value of the environment variable `NUM_DOCS_PER_BATCH` (L126 of the
`run.sh` script). On [a fairly powerful laptop at the time of  
writing](https://www.lenovo.com/gb/en/p/laptops/thinkpad/thinkpadx1/x1-extreme-gen-2/22tp2txx1e2) 20,000 has been 
found to be a reliable number via painstaking trail and error, but your mileage may vary. Ensure that there are no 
errors in the script logs, or### Update test data
Just add the necessary species names and experiment accessions in the `test-dev.env` file and rebuild the development 
environment. Some tests may fail due to incomplete annotations; `grep` for `DistributedUpdatesAsyncException` in 
particular, which signals a problem storing the document batch, which in turn stops processing the current file. If 
found, try again with a lower value for `NUM_DOCS_PER_BATCH`.

### Update test data
Add or change the necessary species names and experiment accessions in the `test-data.env` file and rebuild the 
development environment.

## Testing

### TL;DR
```bash
./execute-all-tests.sh 18      # For non-anndata DB
./execute-all-tests.sh latest  # For anndata DB
./execute-single-test.sh TEST_NAME [ 18 ]
./debug-single-test.sh TEST_NAME [ 18 ]
./stop-and-remove-containers.sh
```

### Execute all tests
The `scxa-gradle` service in `docker/docker-compose-gradle.yml` executes all tests and writes reports to 
`atlas-web-core/build` and `app/build` in the host machine. It requires the SolrCloud service described earlier, and a 
Postgres container with the following differences compared to the development service, `scxa-postgres`: it doesn’t use 
named volumes to ensure the database is clean before running any tests, and its name (as well as the dependency 
declared in `docker-compose-gradle.yml`) has been changed to `scxa-postgres-test`. We don’t want to use 
`scxa-postgres` by mistake and wipe the tables from the dev instance when cleaning fixtures... such an unfortunate 
accident is known to have happened.

The job is split in the following six phases:
1. Clean build directory
2. Compile test classes
3. Run unit tests
4. Run integration tests
5. Run end-to-end tests
6. Generate JaCoCo reports

You should specify which `SCHEMA_VERSION` you want to test against (i.e. *anndata* or non-*anndata* database, see the 
PostgreSQL section above).

For non-*anndata*:
```bash
SCHEMA_VERSION=18 \
docker compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
up
```

For *anndata*:
```bash
SCHEMA_VERSION=latest \
docker compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
up
```

You will eventually see these log messages:
```
scxa-gradle         | BUILD SUCCESSFUL in 2s
scxa-gradle         | 3 actionable tasks: 1 executed, 2 up-to-date
scxa-gradle exited with code 0
```

Press `Ctrl+C` to stop the container and clean any leftovers:
```bash
SCHEMA_VERSION=18 \
docker compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
down
```

Or run `./stop-and-remove-containers.sh`.

You will find very convenient to use the script `execute-all-tests.sh`. By default, it runs the *anndata* database, but
the schema version can be provided as an argument. E.g.:
```bash
./execute-all-tests.sh [ 18 ]
```

The script uses `docker compose run`, and control returns to your shell once the tasks have finished.

### Execute a single test
Many times you will find yourself working in a specific test case or class. Running all tests in such cases is
impractical. In such situations you can use
[Gradle’s continuous build execution](https://blog.gradle.org/introducing-continuous-build). See the example below for
e.g. `ExperimentFileLocationServiceIT.java`:
```bash
docker compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
run --rm --service-ports \
scxa-gradle bash -c '
./gradlew :app:clean &&
./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
app:testClasses &&
./gradlew --continuous :app:test --tests ExperimentFileLocationServiceIT
'
```

After running the test Gradle stays idle and waits for any changes in the code. When it detects that the files in your
project have been updated it will recompile them and run the specified test again. Notice that you can specify multiple 
test files after `--tests` (by name or with wildcards).

Again, a convenience script can be used:
```bash
./execute-single-test.sh TEST_NAME [ 18 ]
```

The second argument is optional and can be used to specify a database version. As before, if unspecified it defaults to
`latest`.

### Debug tests
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

The script `debug-single-test.sh` is a shortcut for this task. It takes the same arguments as executing a single test.

## Run web application
The web application is compiled in two stages:
1. Front end JavaScript packages are transpiled into “bundles” with [Webpack](https://webpack.js.org/)
2. Bundles and back end Java code are built as a WAR file

Lastly, Tomcat deploys the WAR file according to `app/src/main/webapp/META-INF/context.xml`; other Java EE web servers
might work but no testing has been carried out in this regard.

For the first step you can run the following script:
```bash
 ./compile-front-end-packages.sh -iu
```

The sceond step is simply:
```bash
./gradlew :app:war
```

The script `build-and-deploy-webapp.sh` puts it altogether and will eventually launch a Tomcat container with a running
dev instance of Single Cell Expression Atlas.



### Gradle shell
 It
also creates a service with one Gradle container that you can attach to your terminal:
```bash
docker attach scxa-gradle-shell-1
```

You can run tests within and any Gradle task in that container (a running Gradle daemon is provided). Every time you 
re-run the `war` task (e.g. in the Gradle shell container) the web app will be automatically re-deployed by Tomcat.


You can also set a Docker Compose *Run* configuration in IntelliJ IDEA with `SCHEMA_VERSION` in environment variables
and `dev.env` in environment files.
