package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.download.ExperimentFileType;
import uk.ac.ebi.atlas.download.IconType;
import uk.ac.ebi.atlas.experimentpage.cellplot.CellPlotService;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGeneService;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.model.resource.AtlasResource;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.resource.DataFileHub.SingleCellExperimentFiles;
import uk.ac.ebi.atlas.search.OntologyAccessionsSearchService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.net.URI;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExperimentPageContentServiceTest {
    private static final Random RNG = ThreadLocalRandom.current();

    private static final String EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE =
            "experiment/abc/download/zip?fileType=xyx&accessKey=efg";
    private static final String EXPERIMENT_FILES_URI_TEMPLATE =
            "experiment/abc/download?fileType=xyz&accessKey=efg";
    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();
    private final JsonObject tpmsDownloadJsonObject = new JsonObject();
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
    @Mock
    private CellPlotService cellPlotServiceMock;
    @Mock
    private MarkerGeneService markerGeneServiceMock;
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

        when(tsnePlotSettingsServiceMock.getAvailableKs(anyString()))
                .thenReturn(ImmutableList.of(1, 2, 3));
        when(tsnePlotSettingsServiceMock.getKsWithMarkerGenes(anyString()))
                .thenReturn(ImmutableList.of("1", "2"));
        when(tsnePlotSettingsServiceMock.getExpectedClusters(anyString()))
                .thenReturn(Optional.of(1));
        when(tsnePlotSettingsServiceMock.getAvailablePerplexities(anyString()))
                .thenReturn(ImmutableList.of(1, 2, 3));
        when(cellMetadataServiceMock.getMetadataTypes(anyString()))
                .thenReturn(ImmutableSet.of("foo", "bar", "foo bar"));
        when(cellMetadataServiceMock.getMetadataValuesForGivenType(anyString(), anyString()))
                .thenReturn(ImmutableMap.of());

        subject = new ExperimentPageContentService(
                experimentFileLocationServiceMock,
                dataFileHubMock,
                tsnePlotSettingsServiceMock,
                cellMetadataServiceMock,
                ontologyAccessionsSearchService,
                experimentTraderMock,
                cellPlotServiceMock,
                markerGeneServiceMock);

        tpmsDownloadJsonObject.addProperty("url", EXPERIMENT_FILES_ARCHIVE_URI_TEMPLATE);
        tpmsDownloadJsonObject.addProperty("type", IconType.TSV.getName());
        tpmsDownloadJsonObject.addProperty("description", "Filtered TPMs files (MatrixMarket archive)");
        tpmsDownloadJsonObject.addProperty("isDownload", true);
    }

    private void setupCommonExperimentMocks(String experimentAccession) {
        for (var type : ImmutableList.of(
                ExperimentFileType.EXPERIMENT_METADATA,
                ExperimentFileType.EXPERIMENT_DESIGN,
                ExperimentFileType.CLUSTERING,
                ExperimentFileType.MARKER_GENES,
                ExperimentFileType.NORMALISED
        )) {
            when(experimentFileLocationServiceMock.getFileUri(experimentAccession, type, ""))
                    .thenReturn(URI.create(EXPERIMENT_FILES_URI_TEMPLATE));
        }
    }

    private void setupFileExistenceMock(String experimentAccession, boolean clusteringExists) {
        var singleCellFilesMock = mock(SingleCellExperimentFiles.class);
        var clustersTsvMock = mock(AtlasResource.class);

        when(dataFileHubMock.getSingleCellExperimentFiles(experimentAccession))
                .thenReturn(singleCellFilesMock);
        when(singleCellFilesMock.getClustersTsv()).thenReturn(clustersTsvMock);
        when(clustersTsvMock.exists()).thenReturn(clusteringExists);
    }

    @Test
    void testGetDownloadsForANNDExperiment_withClustering() {
        var experimentAccession = "E-ANND-123";
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(experimentAccession)
                .withTechnologyType(ImmutableList.of("Smart-Seq", "10xV1"))
                .build();
        when(experimentTraderMock.getExperiment(experimentAccession, "")).thenReturn(experiment);

        setupCommonExperimentMocks(experimentAccession);
        setupFileExistenceMock(experimentAccession, true);

        var downloads = subject.getDownloads(experimentAccession, "");

        assertThat(downloads)
                .hasSize(2)
                .filteredOn(jsonElement -> jsonElement.getAsJsonObject().get("title").getAsString().equalsIgnoreCase("Result Files"))
                .hasSize(1)
                .extracting(jsonElement -> jsonElement.getAsJsonObject().get("files").getAsJsonArray())
                .hasSize(1)
                .first()
                .satisfies(jsonArray -> {
                    assertThat(jsonArray).hasSize(3);
                });
    }

    @Test
    void testGetDownloadsForANNDExperiment_withoutClustering() {
        var experimentAccession = "E-ANND-123";
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(experimentAccession)
                .withTechnologyType(ImmutableList.of("Smart-Seq", "10xV1"))
                .build();
        when(experimentTraderMock.getExperiment(experimentAccession, "")).thenReturn(experiment);

        setupCommonExperimentMocks(experimentAccession);
        setupFileExistenceMock(experimentAccession, false);

        var downloads = subject.getDownloads(experimentAccession, "");

        assertThat(downloads)
                .hasSize(2)
                .filteredOn(jsonElement -> jsonElement.getAsJsonObject().get("title").getAsString().equalsIgnoreCase("Result Files"))
                .hasSize(1)
                .extracting(jsonElement -> jsonElement.getAsJsonObject().get("files").getAsJsonArray())
                .hasSize(1)
                .first()
                .satisfies(jsonArray -> {
                    assertThat(jsonArray).hasSize(2);
                });
    }

    @Test
    void smartExperimentsHaveTPMDownloadFiles() {
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(EXPERIMENT_ACCESSION)
                .withTechnologyType(ImmutableList.of("Smart-Seq", "10xV1"))
                .build();

        when(experimentTraderMock.getExperiment(EXPERIMENT_ACCESSION, "")).thenReturn(experiment);

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

        when(experimentTraderMock.getExperiment(EXPERIMENT_ACCESSION, "")).thenReturn(experiment);

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
    void anatomogramDoesNotExistForValidExperiment() {
        var result = this.subject.getTsnePlotData("E-CURD-10");
        assertThat(result.getAsJsonObject("anatomogram").size()).isEqualTo(0);
    }

    @Test
    void givenInvalidExperiment_thenReturnsEmptyDefaultPlotMethodAndParams() {
        String invalidExperimentAccession = "FooBar";
        when(cellPlotServiceMock.fetchDefaultPlotMethodWithParameterisation(invalidExperimentAccession))
                .thenReturn(ImmutableMap.of());

        assertThat(subject.fetchDefaultPlotMethodAndParameterisation(invalidExperimentAccession)).isEmpty();
    }

    @Test
    void getEmptyDefaultPlotMethodAndParamsForTheValidExperiment() {
        when(cellPlotServiceMock.fetchDefaultPlotMethodWithParameterisation("E-CURD-4"))
                .thenReturn(ImmutableMap.of("umap", new Gson().fromJson("{\"n_neighbors\":100}", JsonObject.class),
                        "tsne", new Gson().fromJson("{\"perplexity\":50}", JsonObject.class)));

        assertThat(subject.fetchDefaultPlotMethodAndParameterisation("E-CURD-4")).isNotEmpty();
    }
}
