package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.search.OntologyAccessionsSearchService;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExperimentPageContentServiceIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private ExperimentFileLocationService experimentFileLocationService;

    @Inject
    private DataFileHub dataFileHub;

    @Inject
    private TSnePlotSettingsService tsnePlotSettingsService;

    @Inject
    private CellMetadataService cellMetadataService;

    @Inject
    private OntologyAccessionsSearchService ontologyAccessionsSearchService;

    @Inject
    private ExperimentTrader experimentTrader;

    private ExperimentPageContentService subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.setScripts(
                new ClassPathResource("fixtures/202108/experiment.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics.sql"),
                new ClassPathResource("fixtures/202203/scxa_coords.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group_membership.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.setScripts(
                new ClassPathResource("fixtures/202108/experiment-delete.sql"),
                new ClassPathResource("fixtures/202108/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/202108/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/202108/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/202108/scxa_cell_group_membership-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject =
                new ExperimentPageContentService(
                        experimentFileLocationService,
                        dataFileHub,
                        tsnePlotSettingsService,
                        cellMetadataService,
                        ontologyAccessionsSearchService,
                        experimentTrader);
    }

    @Test
    void getValidExperimentDesignJson() {
        // TODO replace empty experiment design table with mock table
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var result = this.subject.getExperimentDesign(experimentAccession, new JsonObject(), "");
        assertThat(result.has("table")).isTrue();
        assertThat(result.has("downloadUrl")).isTrue();
    }

    @Test
    void getValidAnalysisMethodsJson() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var result = this.subject.getAnalysisMethods(experimentAccession);

        // Should have header row and at least one other
        assertThat(result.size()).isGreaterThan(1);

        var headerRow = result.get(0).getAsJsonArray();
        assertThat(headerRow)
                .hasSize(4)
                .containsExactlyInAnyOrder(
                        new JsonPrimitive("Analysis"),
                        new JsonPrimitive("Software"),
                        new JsonPrimitive("Version"),
                        new JsonPrimitive("Citation"));
    }

    @Test
    void getValidDownloadsJson() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var result = this.subject.getDownloads(experimentAccession, "");

        assertThat(result).hasSize(2);

        for (var download : result) {
            var downloadObject = download.getAsJsonObject();

            assertThat(downloadObject.get("title").getAsString()).isNotEmpty();

            var downloadFiles = downloadObject.get("files").getAsJsonArray();

            assertThat(downloadFiles).isNotEmpty();

            for (var file : downloadFiles) {
                var fileObject = file.getAsJsonObject();

                assertThat(fileObject.has("url")).isTrue();
                assertThat(fileObject.get("url").getAsString()).isNotEmpty();

                assertThat(fileObject.has("type")).isTrue();
                assertThat(fileObject.get("type").getAsString()).isNotEmpty();

                assertThat(fileObject.has("description")).isTrue();
                assertThat(fileObject.get("description").getAsString()).isNotEmpty();

                assertThat(fileObject.has("isDownload")).isTrue();
                assertThat(fileObject.get("isDownload").getAsBoolean()).isTrue();
            }
        }
    }

    @Test
    void getValidTsnePlotDataJson() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var result = this.subject.getTsnePlotData(experimentAccession);

        assertThat(result.has("suggesterEndpoint")).isTrue();
        assertThat(result.get("suggesterEndpoint").getAsString()).isEqualToIgnoringCase("json/suggestions");

        assertThat(result.has("ks")).isTrue();
        assertThat(
                ImmutableSet.copyOf(result.get("ks").getAsJsonArray()).stream()
                        .map(JsonElement::getAsInt)
                        .collect(toSet()))
                .containsExactlyInAnyOrder(
                        jdbcTestUtils.fetchKsFromCellGroups(experimentAccession).toArray(new Integer[0]));

        if (result.has("selectedK")) {
            assertThat(jdbcTestUtils.fetchKsFromCellGroups(experimentAccession))
                    .contains(result.get("selectedK").getAsInt());
        }

        assertThat(result.has("plotTypesAndOptions")).isTrue();
        assertThat(result.get("plotTypesAndOptions").getAsJsonObject().get("tsne").getAsJsonArray()).isNotEmpty();
        assertThat(result.get("plotTypesAndOptions").getAsJsonObject().get("umap").getAsJsonArray()).isNotEmpty();

        // Not all experiments have metadata, see E-GEOD-99058
        if (result.has("metadata")) {
            assertThat(result.get("metadata").getAsJsonArray())
                    .doesNotHaveDuplicates();
        }

        assertThat(result.has("units")).isTrue();
        assertThat(result.get("units").getAsJsonArray()).isNotEmpty();
    }

    @Test
    void anatomogramExistsForValidExperiment() {
        var experimentAccession = "E-MTAB-5061";
        var result = this.subject.getTsnePlotData(experimentAccession);

        assertThat(result.has("anatomogram")).isTrue();
    }
}
