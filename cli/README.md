# Expression Atlas CLI Bulk
A minimal Spring Boot wrapper to run Single Cell Expression Atlas tasks from the command line.

## Requirements
- Java 11
- Expression Atlas environment (PostgreSQL server; SolrCloud cluster; bioentity annotation and experiment files)

## Usage
There are two main ways to run the application: as an executable JAR or via Gradle. The latter is recommended on
development environments and Java is preferred in production environments. Be aware that any changes made to the
properties file won’t take effect unless you rebuild the JAR file.

### Gradle
```bash
./gradlew :cli:bootRun --args="<task-name> <options>"
```

### Executable JAR
Build the JAR file:
```bash
./gradlew :cli:bootJar
```

Then run it with Java:
```bash
java -jar ./cli/build/libs/atlas-cli-sc.jar <task-name> <options>
```

## Configuration
Configuration variables are set with `-Dproperty=value` if you run the application via `java -jar ...`, or by adding
`-Pproperty=value` to the Gradle task (in the tables below: Java property name, and Gradle propery name, respectively).

**IMPORTANT**: At the very least you will need to set the environment variables described in the Default value columns
to run/compile the application with Gradle. However, notice that the `-D` arguments will override whatever was set at
compile time, so if you forget or your environment changes, you don’t need to recompile.

### Expression Atlas file options: `configuration.properties`
| Java property name          | Gradle property name      | Default value            |
|-----------------------------|---------------------------|--------------------------|
| `data.files.location`       | `dataFilesLocation`       | `${ATLAS_DATA_PATH}`     |
| `experiment.files.location` | `experimentFilesLocation` | `${ATLAS_DATA_PATH}/gxa` |

### Expression Atlas database options: `jdbc.properties`
| Java Property name | Gradle property name | Default value                                                       |
|--------------------|----------------------|---------------------------------------------------------------------|
| `jdbc.url`         | `jdbcUrl`            | `jdbc:postgresql://${ATLAS_POSTGRES_HOST}:5432/${ATLAS_POSTGRES_DB` |
| `jdbc.username`    | `jdbcUsername`       | `${ATLAS_POSTGRES_USER}`                                            |
| `jdbc.password`    | `jdbcPassword`       | `${ATLAS_POSTRES_PASSWORD}`                                         |

### Expression Atlas Solr options: `solr.properties`
| Java property name | Gradle property name | Default value        |
|--------------------|----------------------|----------------------|
| `zk.host`          | `zkHost`             | `${ATLAS_ZK_HOST}`   |
| `zk.port`          | `zkPort`             | `2181`               |
| `solr.host`        | `solrHost`           | `${ATLAS_SOLR_HOST}` |
| `solr.port`        | `solrPort`           | `8983`               |

## Tasks
Run without any arguments to get a list of available tasks:
```
Usage: <main class> [COMMAND]
Commands:
  update-experiment-design  Updates the experiment designs of an accession or a group of accessions.
```

Pass the name of a task to obtain a detailed description of available options:
```bash
$ java -jar ./cli/build/libs/atlas-cli-bulk.jar update-experiment-design
...

```

