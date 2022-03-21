package uk.ac.ebi.atlas.experimentpage.metadata;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.StringUtils;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellMetadataDaoIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(CellMetadataDaoIT.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory;

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private IdfParser idfParser;

    @Inject
    private CondensedSdrfParser condensedSdrfParser;

    @Inject
    private DataFileHub dataFileHub;

    private static final String IDF_ADDITIONAL_ATTRIBUTES_ID = "Comment[EAAdditionalAttributes]".toUpperCase();

    private CellMetadataDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/experiment.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/experiment-delete.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new CellMetadataDao(solrCloudCollectionProxyFactory, idfParser);
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validExperimentAccessionHasMetadataFields(String experimentAccession) {
        assertThat(subject.getFactorTypes(experimentAccession)).isNotEmpty();
    }

    @Test
    void invalidExperimentAccessionHasNoMetadata() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();

        assertThat(subject.getFactorTypes(experimentAccession)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validExperimentAccessionHasFactorFields(String experimentAccession) {
        var cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        LOGGER.info("Retrieving factor fields for cell ID {} from experiment {}", cellId, experimentAccession);
        assertThat(subject.getFactorTypes(experimentAccession, cellId)).isNotEmpty();
    }

    @Test
    void invalidCellIdAndExperimentAccessionHasNoFactorFields() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var cellId = "FOO";

        assertThat(subject.getFactorTypes(experimentAccession, cellId)).isEmpty();
    }

    @Test
    void experimentWithMissingValuesReturnsNotAvailable() {
        var experimentAccession = "E-GEOD-71585";
        var result = subject.getMetadataValues(experimentAccession, "inferred_cell_type_-_ontology_labels");

        // Another way to test this would be to check that the result has fewer cells than the experiment, and that the
        // missing cell IDs don’t have a value for the specified metadata value
        assertThat(result)
                .doesNotContainKeys("SRR2138737", "SRR2140225", "SRR2139550", "SRR2139566");
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validCellIdHasMetadataValues(String experimentAccession) {
        var cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        var factors = subject.getFactorTypes(experimentAccession, cellId);
        var characteristics = subject.getCharacteristicTypes(experimentAccession);

        var result = subject.getMetadataValuesForCellId(experimentAccession, cellId, factors, characteristics);

        assertThat(result)
                .isNotEmpty()
                .extracting("inferred_cell_type_-_ontology_labels")
                .isNotEmpty();
    }

    @Test
    void validCellIdHasNoMetadataValuesForNoMetadataTypes() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        assertThat(
                subject.getMetadataValuesForCellId(experimentAccession, cellId, ImmutableSet.of(), ImmutableSet.of()))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validExperimentAccessionHasMetadataValues(String experimentAccession) {
        var condensedSdrfParserOutput =
                condensedSdrfParser.parse(experimentAccession, SINGLE_CELL_RNASEQ_MRNA_BASELINE);

        var factorTypes = subject.getFactorTypes(experimentAccession);
        assertThat(factorTypes)
                .isNotEmpty()
                .allSatisfy(factor ->
                    assertThat(subject.getMetadataValues(experimentAccession, factor).keySet())
                            .isNotEmpty()
                            // Samples in the condensed SDRF can have missing attributes
                            .containsAnyElementsOf(
                                    condensedSdrfParserOutput.getExperimentDesign().getAllRunOrAssay()));
    }

    @ParameterizedTest
    @MethodSource("experimentsWithAdditionalAttributesProvider")
    void validExperimentIdHasAdditionalAttributes(String experimentAccession) {
        assertThat(subject.getCharacteristicTypes(experimentAccession)).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithoutAdditionalAttributesProvider")
    void invalidExperimentAccessionHasNoAdditionalAttributes(String experimentAccession) {
        assertThat(subject.getCharacteristicTypes(experimentAccession)).isEmpty();
    }

    @Test
    void noResultsAreReturnedForInvalidIds() {
        assertThat(
                subject.getMetadataValuesForCellId(
                        "FOO",
                        "BAR",
                        emptyList(),
                        Collections.singletonList("organism")))
                .isEmpty();
    }

    private Stream<String> experimentsWithFactorsProvider() {
        // E-GEOD-99058 and E-GEOD-81547 have only single cell identifier factor (no inferred cell types)
        // E-CURD-4 Not all samples from the cond. SDRF have factors: they’re artifacts from sequencing, not cell IDs
        return jdbcUtils.fetchPublicExperimentAccessions()
                .stream()
                .filter(accession -> !accession.equalsIgnoreCase("E-GEOD-99058") &&
                                     !accession.equalsIgnoreCase("E-GEOD-81547") &&
                                     !accession.equalsIgnoreCase("E-CURD-4"));
    }

    private Stream<String> experimentsWithAdditionalAttributesProvider() {
        return jdbcUtils.fetchPublicExperimentAccessions()
                .stream()
                .filter(this::hasAdditionalAttributesInIdf);
    }

    private Stream<String> experimentsWithoutAdditionalAttributesProvider() {
        return jdbcUtils.fetchPublicExperimentAccessions()
                .stream()
                .filter(accession -> !hasAdditionalAttributesInIdf(accession));
    }

    private boolean hasAdditionalAttributesInIdf(String experimentAccession) {
        try (TsvStreamer idfStreamer = dataFileHub.getExperimentFiles(experimentAccession).idf.get()) {
            var additionalAttributesLine = idfStreamer
                    .get()
                    .filter(line -> StringUtils.trimAllWhitespace(line[0]).equalsIgnoreCase(IDF_ADDITIONAL_ATTRIBUTES_ID))
                    .map(line -> Arrays.stream(line)
                            .skip(1)
                            .filter(item -> !item.isEmpty())
                            .collect(Collectors.toList()))
                    .filter(x -> !x.isEmpty())
                    .findFirst();

            return additionalAttributesLine.isPresent();
        }
    }
}
