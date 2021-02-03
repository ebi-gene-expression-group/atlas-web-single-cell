package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.download.ExperimentFileType;
import uk.ac.ebi.atlas.download.IconType;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.search.OntologyAccessionsSearchService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExperimentPageContentServiceTest {

    private static final String EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE =
            "experiment/abc/download/zip?fileType=xyx&accessKey=efg";
    private static final String EXPERIMENT_FILES_URI_TEMPLATE =
            "experiment/abc/download?fileType=xyz&accessKey=efg";
    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();
    private static final String NON_ANATOMOGRAM_EXPERIMENT_ACCESSION = "E-GEOD-130473";

    @Mock
    private ExperimentFileLocationService experimentFileLocationServiceMock;

    @Mock
    private DataFileHub dataFileHubMock;

    @Mock
    private TSnePlotSettingsService tsnePlotSettingsServiceMock;

    @Mock
    private CellMetadataService cellMetadataServiceMock;

    @Mock
    private OntologyAccessionsSearchService ontologyAccessionsSearchService;

    @Mock
    private ExperimentTrader experimentTraderMock;

    private JsonObject tpmsDownloadJsonObject = new JsonObject();

    private ExperimentPageContentService subject;

    @BeforeEach
    void setUp() {
        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.EXPERIMENT_METADATA,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.EXPERIMENT_DESIGN,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.QUANTIFICATION_FILTERED,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.CLUSTERING,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.MARKER_GENES,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.NORMALISED,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(experimentFileLocationServiceMock.getFileUri(
                EXPERIMENT_ACCESSION,
                ExperimentFileType.QUANTIFICATION_RAW,
                "")
        ).thenReturn(URI.create(EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE));

        when(tsnePlotSettingsServiceMock.getAvailableKs(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION))
                .thenReturn(ImmutableList.of(1, 2, 3));
        when(tsnePlotSettingsServiceMock.getKsWithMarkerGenes(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION))
                .thenReturn(ImmutableList.of("1", "2"));
        when(tsnePlotSettingsServiceMock.getExpectedClusters(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION))
                .thenReturn(Optional.of(1));
        when(tsnePlotSettingsServiceMock.getAvailablePerplexities(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION))
                .thenReturn(ImmutableList.of(1, 2, 3));
        when(cellMetadataServiceMock.getMetadataTypes(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION))
                .thenReturn(ImmutableSet.of("foo"));
        when(cellMetadataServiceMock.getMetadataValuesForGivenType(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION, "foo"))
                .thenReturn(ImmutableMap.of());

        subject = new ExperimentPageContentService(
                experimentFileLocationServiceMock,
                dataFileHubMock,
                tsnePlotSettingsServiceMock,
                cellMetadataServiceMock,
                ontologyAccessionsSearchService,
                experimentTraderMock);

        tpmsDownloadJsonObject.addProperty("url", EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE);
        tpmsDownloadJsonObject.addProperty("type", IconType.TSV.getName());
        tpmsDownloadJsonObject.addProperty("description", "Filtered TPMs files (MatrixMarket archive)");
        tpmsDownloadJsonObject.addProperty("isDownload", true);
    }

    @Test
    void smartExperimentsHaveTPMDownloadFiles() {
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(EXPERIMENT_ACCESSION)
                .withTechnologyType(ImmutableList.of("Smart-Seq", "10xV1"))
                .build();

        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION)).thenReturn(experiment);

        var result = subject.getDownloads(EXPERIMENT_ACCESSION, "");
        assertThat(result)
                .hasSize(2)
                .filteredOn(jsonElement -> jsonElement.getAsJsonObject().get("title").getAsString().equalsIgnoreCase("Result Files"))
                .hasSize(1)
                .extracting(jsonElement -> jsonElement.getAsJsonObject().get("files").getAsJsonArray())
                .hasSize(1)
                .first()
                .satisfies(jsonArray -> {
                    assertThat(jsonArray).hasSize(5);
                    assertThat(jsonArray).contains(tpmsDownloadJsonObject);
                });
    }

    @Test
    void nonSmartExperimentsDoesNotHaveTPMDownloadFiles() {
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(EXPERIMENT_ACCESSION)
                .withTechnologyType(ImmutableList.of("10xV1"))
                .build();

        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION)).thenReturn(experiment);

        var result = subject.getDownloads(EXPERIMENT_ACCESSION, "");
        assertThat(result)
                .hasSize(2)
                .filteredOn(jsonElement -> jsonElement.getAsJsonObject().get("title").getAsString().equalsIgnoreCase("Result Files"))
                .hasSize(1)
                .extracting(jsonElement -> jsonElement.getAsJsonObject().get("files").getAsJsonArray())
                .hasSize(1)
                .first()
                .satisfies(jsonArray -> {
                    assertThat(jsonArray).hasSize(4);
                    assertThat(jsonArray).doesNotContain(tpmsDownloadJsonObject);
                });
    }

    @Test
    void anatomogramDoesNotExistsForValidExperiment() {
        assertThat(this.subject
                .getTsnePlotData(NON_ANATOMOGRAM_EXPERIMENT_ACCESSION)
                .has("anatomogram"))
                .isFalse();
    }
}
