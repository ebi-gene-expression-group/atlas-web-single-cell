package uk.ac.ebi.atlas.experimentpage.tabs;


import com.google.common.collect.ImmutableList;
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
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.net.URI;

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

    @Mock
    private ExperimentFileLocationService experimentFileLocationServiceMock;

    @Mock
    private DataFileHub dataFileHubMock;

    @Mock
    private TSnePlotSettingsService tsnePlotSettingsServiceMock;

    @Mock
    private CellMetadataService cellMetadataServiceMock;

    @Mock
    private ExperimentTrader experimentTraderMock;

    private ExperimentPageContentService subject;


    @BeforeEach
    public void setUp() {
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

        subject = new ExperimentPageContentService(
                experimentFileLocationServiceMock,
                dataFileHubMock,
                tsnePlotSettingsServiceMock,
                cellMetadataServiceMock,
                experimentTraderMock
        );
    }

    @Test
    void smartExperimentsHaveTPMDownloadFiles() {
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(EXPERIMENT_ACCESSION)
                .withTechnologyType(ImmutableList.of("Smart-Seq", "10xV1"))
                .build();

        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION)
        ).thenReturn(experiment);

        var fileObject = new JsonObject();
        fileObject.addProperty("url", "experiment/abc/download/zip?fileType=xyx&accessKey=efg");
        fileObject.addProperty("type", "icon-tsv");
        fileObject.addProperty("description", "Filtered TPMs files (MatrixMarket archive)");
        fileObject.addProperty("isDownload", true);

        var result = subject.getDownloads(EXPERIMENT_ACCESSION, "");

        assertThat(result).hasSize(2);

        for (var download : result) {
            var downloadObject = download.getAsJsonObject();
            if (downloadObject.get("title").getAsString().equalsIgnoreCase("Result Files")) {
                var downloadFiles = downloadObject.get("files").getAsJsonArray();

                assertThat(downloadFiles).isNotEmpty();
                assertThat(downloadFiles).size().isEqualTo(5);
                assertThat(downloadFiles).contains(fileObject);
            }
        }
    }

    @Test
    void nonSmartExperimentsDoesNotHaveTPMDownloadFiles() {
        var experiment = new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                .withExperimentAccession(EXPERIMENT_ACCESSION)
                .withTechnologyType(ImmutableList.of("10xV1"))
                .build();

        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION)
        ).thenReturn(experiment);

        var fileObject = new JsonObject();
        fileObject.addProperty("url", "experiment/abc/download/zip?fileType=xyx&accessKey=efg");
        fileObject.addProperty("type", "icon-tsv");
        fileObject.addProperty("description", "Filtered TPMs files (MatrixMarket archive)");
        fileObject.addProperty("isDownload", true);

        var result = subject.getDownloads(EXPERIMENT_ACCESSION, "");

        assertThat(result).hasSize(2);

        for (var download : result) {
            var downloadObject = download.getAsJsonObject();
            if (downloadObject.get("title").getAsString().equalsIgnoreCase("Result Files")) {
                var downloadFiles = downloadObject.get("files").getAsJsonArray();

                assertThat(downloadFiles).isNotEmpty();
                assertThat(downloadFiles).size().isEqualTo(4);
                assertThat(downloadFiles).doesNotContain(fileObject);
            }
        }
    }
}