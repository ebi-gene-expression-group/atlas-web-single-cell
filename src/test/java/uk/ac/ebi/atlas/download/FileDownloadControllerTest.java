package uk.ac.ebi.atlas.download;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FileDownloadControllerTest {

    private static final List<String> EXPERIMENT_ACCESSION_LIST = ImmutableList.of("E-MTAB-5061", "E-EHCA-2");

    @Mock
    private ExperimentFileLocationService experimentFileLocationServiceMock;

    @Mock
    private ExperimentTrader experimentTraderMock;

    private FileDownloadController subject;

    @Before
    public void setUp() {
        subject = new FileDownloadController(experimentFileLocationServiceMock, experimentTraderMock);
    }

    @Test
    public void testInvalidFilesForDownloading() {

        Experiment experiment = mock(Experiment.class);
        when(experiment.getAccession()).thenReturn(EXPERIMENT_ACCESSION_LIST.get(0));
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION_LIST.get(0))).thenReturn(experiment);

        Experiment experiment2 = mock(Experiment.class);
        when(experiment2.getAccession()).thenReturn(EXPERIMENT_ACCESSION_LIST.get(1));
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION_LIST.get(1))).thenReturn(experiment2);

        String textPath = "file:dir/filename";
        Path path = Paths.get(textPath);

        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.EXPERIMENT_DESIGN))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.EXPERIMENT_DESIGN))
               .thenReturn(path);

        when(experimentFileLocationServiceMock.getFilePathsForArchive(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.QUANTIFICATION_RAW))
                .thenReturn(ImmutableList.of(path, path, path));
        when(experimentFileLocationServiceMock.getFilePathsForArchive(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.QUANTIFICATION_RAW))
                .thenReturn(ImmutableList.of(path, path, path));

        when(experimentFileLocationServiceMock.getFilePathsForArchive(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.NORMALISED))
                .thenReturn(ImmutableList.of(path, path, path));
        when(experimentFileLocationServiceMock.getFilePathsForArchive(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.NORMALISED))
                .thenReturn(ImmutableList.of(path, path, path));

        String jsonResponse = subject.checkMultipleExperimentsFileValid(EXPERIMENT_ACCESSION_LIST);
        ReadContext ctx = JsonPath.parse(jsonResponse);

        assertThat(ctx.<Map<String, Object>>read("$"))
                .extracting("invalidFiles")
                .extracting(EXPERIMENT_ACCESSION_LIST.get(0), EXPERIMENT_ACCESSION_LIST.get(1))
                .contains(
                        tuple(List.of("filename", "filename", "filename", "filename", "filename", "filename", "experiment-design"),
                                List.of("filename", "filename", "filename", "filename", "filename", "filename", "experiment-design")));

    }

}
