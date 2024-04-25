# Open API

This subproject generates Open API documentation for all controllers in `app` and `atlas-web-ocre` subprojects. It is
powered by [springdoc-openapi](https://springdoc.org/) and 
[its Gradle plugin](https://github.com/springdoc/springdoc-openapi-gradle-plugin). It is a Spring Boot minimal web app
that includes the two mentioned projects and finds all the relevant URLs by virtue of package scanning.

## Requirements
- Java 11
- Expression Atlas environment (PostgreSQL server; SolrCloud cluster; bioentity annotation and experiment files)

## Usage
### Swagger UI
```bash
./gradlew :openapi:bootRun
```

Launch your browser and point it at `http://localhost:8081/swagger-ui.html`. Also, you can find your API docs at 
`http://localhost:8081/v3/api-docs.yaml` (YAML) or `http://localhost:8081/v3/api-docs` (JSON).

### Generate API docs
```bash
./gradlew :openapi:generateOpenApiDocs
```

Find your Open API doc at `openapi/build/openapi.yaml`.

## Configuration
Configuration variables are set with `-Dproperty=value` if you run the application via `java -jar ...`, or by adding
`-Pproperty=value` to the Gradle task (in the tables below: Java property name, and Gradle propery name, respectively).

**IMPORTANT**: At the very least you will need to set the environment variables described in the Default value columns
to run/compile the application with Gradle. However, notice that the `-D` arguments will override whatever was set at
compile time, so if you forget or your environment changes, you don’t need to recompile.

### Expression Atlas file options: `configuration.properties`
| Java property name          | Gradle property name      | Default value               |
|-----------------------------|---------------------------|-----------------------------|
| `data.files.location`       | `dataFilesLocation`       | `${ATLAS_DATA_PATH}`        |
| `experiment.files.location` | `experimentFilesLocation` | `${ATLAS_EXPERIMENTS_PATH}` |

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