package uk.ac.ebi.atlas.metadata;

import com.google.common.collect.ImmutableList;
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
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

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
    private DataFileHub dataFileHub;

    private static final String IDF_ADDITIONAL_ATTRIBUTES_ID = "Comment[EAAdditionalAttributes]".toUpperCase();

    private CellMetadataDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_analytics-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"));
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
        String experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();

        assertThat(subject.getFactorTypes(experimentAccession)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validExperimentAccessionHasFactorFields(String experimentAccession) {
        String cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        LOGGER.info("Retrieving factor fields for cell ID {} from experiment {}", cellId, experimentAccession);
        assertThat(subject.getFactorTypes(experimentAccession, cellId)).isNotEmpty();
    }

    @Test
    void invalidCellIdAndExperimentAccessionHasNoFactorFields() {
        String experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        String cellId = "FOO";

        assertThat(subject.getFactorTypes(experimentAccession, cellId)).isEmpty();
    }

    @Test
    void experimentWithMissingValuesReturnsNotAvailable() {
        String experimentAccession = "E-GEOD-71585";

        // TODO: Retrieve randomly sampled cell IDs from Solr
        ImmutableList<String> cellIdsWithMissingValues = ImmutableList.of(
                "SRR2138737",
                "SRR2140225",
                "SRR2139550",
                "SRR2139566");

        Map<String, String> result = subject.getMetadataValueForCellIds(experimentAccession,
                "inferred_cell_type",
                cellIdsWithMissingValues);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validCellIdHasMetadataValues(String experimentAccession) {
        String cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        Set<String> factors = subject.getFactorTypes(experimentAccession, cellId);
        Set<String> characteristics = subject.getCharacteristicTypes(experimentAccession);

        Map<String, String> result = subject.getMetadataValuesForCellId(experimentAccession, cellId, factors, characteristics);

        assertThat(result)
                .isNotEmpty()
                .extracting("inferred_cell_type")
                .isNotEmpty();
    }

    @Test
    void validCellIdHasNoMetadataValuesForNoMetadataTypes() {
        String experimentAccession = jdbcUtils.fetchRandomSingleCellExperimentAccession();
        String cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        assertThat(subject.getMetadataValuesForCellId(experimentAccession, cellId, ImmutableSet.of(), ImmutableSet.of()))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithFactorsProvider")
    void validCellIdsHaveMetadataValues(String experimentAccession) {
        Set<String> cellIds = new HashSet<>(jdbcUtils.fetchRandomListOfCellsFromExperiment(
                experimentAccession, ThreadLocalRandom.current().nextInt(1, 2000)));

        LOGGER.info("Retrieving factor types for experiment {}", experimentAccession);

        Set<String> factorTypes = subject.getFactorTypes(experimentAccession);

        assertThat(factorTypes)
                .isNotEmpty()
                .allSatisfy(factor -> {
                    LOGGER.info(
                            "Retrieving values for {} metadata for {} random cell IDs from experiment {}",
                            factor, cellIds.size(), experimentAccession);

                    assertThat(subject.getMetadataValueForCellIds(experimentAccession, factor, cellIds))
                            .isNotEmpty()
                            .containsKeys(cellIds.toArray(new String[0]));
                });
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
    void noMetadataIsRetrievedForEmptyCellIdList() {
        String experimentAccession = jdbcUtils.fetchRandomSingleCellExperimentAccession();

        assertThat(subject.getMetadataValueForCellIds(experimentAccession, "organism", emptyList()))
            .isEmpty();
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
        // E-GEOD-99058 does not have any factors
        return jdbcUtils.fetchPublicSingleCellExperimentAccessions()
                .stream()
                .filter(accession -> !accession.equalsIgnoreCase("E-GEOD-99058"));
    }

    private Stream<String> experimentsWithAdditionalAttributesProvider() {
        return jdbcUtils.fetchPublicSingleCellExperimentAccessions()
                .stream()
                .filter(this::hasAdditionalAttributesInIdf);
    }

    private Stream<String> experimentsWithoutAdditionalAttributesProvider() {
        return jdbcUtils.fetchPublicSingleCellExperimentAccessions()
                .stream()
                .filter(accession -> !hasAdditionalAttributesInIdf(accession));
    }

    private boolean hasAdditionalAttributesInIdf(String experimentAccession) {
        try (TsvStreamer idfStreamer = dataFileHub.getExperimentFiles(experimentAccession).idf.get()) {
            Optional<List<String>> additionalAttributesLine = idfStreamer
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
