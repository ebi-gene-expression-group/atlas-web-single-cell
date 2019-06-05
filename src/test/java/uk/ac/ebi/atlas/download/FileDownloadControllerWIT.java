package uk.ac.ebi.atlas.download;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.isA;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileDownloadControllerWIT {
    private static final String EXPERIMENT_ACCESSION = "E-MTAB-5061";
    private static final List<String> EXPERIMENT_ACCESSION_LIST = ImmutableList.of("E-MTAB-5061", "E-EHCA-2");
    private static final List<String> INVALID_EXPERIMENT_ACCESSION_LIST = ImmutableList.of("E-MTAB", "E-EHCA");
    private static final String EXPERIMENT_DESIGN_FILE_NAME = "ExpDesign-{0}.tsv";
    private static final String ARCHIVE_NAME = "{0}-{1}-files.zip";
    private static final String FILE_DOWNLOAD_URL = "/experiment/{experimentAccession}/download";
    private static final String ARCHIVE_DOWNLOAD_URL = "/experiment/{experimentAccession}/download/zip";
    private static final String ARCHIVE_DOWNLOAD_LIST_URL = "/experiments/download/zip";
    private static final String ARCHIVE_CHECK_LIST_URL = "/experiments/check/zip";

    @Inject
    private DataSource dataSource;

    @Inject
    private ExperimentFileLocationService experimentFileLocationService;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void downloadFileForValidExperimentAccession() throws Exception {
        var expectedFileName = MessageFormat.format(EXPERIMENT_DESIGN_FILE_NAME, EXPERIMENT_ACCESSION);
        this.mockMvc.perform(get(FILE_DOWNLOAD_URL, EXPERIMENT_ACCESSION)
                .param("fileType", ExperimentFileType.EXPERIMENT_DESIGN.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedFileName))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    void downloadFileForInvalidExperimentAccession() throws Exception {
        this.mockMvc.perform(get(FILE_DOWNLOAD_URL, generateRandomExperimentAccession())
                .param("fileType", ExperimentFileType.EXPERIMENT_DESIGN.getId()))
                .andExpect(status().is(404))
                .andExpect(view().name("error-page"));
    }

    @Test
    void downloadFileForInvalidExperimentFileType() throws Exception {
        this.mockMvc.perform(get(FILE_DOWNLOAD_URL, EXPERIMENT_ACCESSION)
                .param("fileType", "foo"))
                .andExpect(status().is(404))
                .andExpect(view().name("error-page"));
    }

    @Test
    void downloadArchiveForValidExperimentAccession() throws Exception {
        var expectedArchiveName =
                MessageFormat.format(
                        ARCHIVE_NAME, EXPERIMENT_ACCESSION, ExperimentFileType.QUANTIFICATION_FILTERED.getId());

        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_URL, EXPERIMENT_ACCESSION)
                .param("fileType", ExperimentFileType.QUANTIFICATION_FILTERED.getId()))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedArchiveName))
                .andExpect(content().contentType("application/zip"));
    }

    @Test
    void downloadMetadataArchiveForValidExperimentAccession() throws Exception {
        var expectedArchiveName =
                MessageFormat.format(
                        ARCHIVE_NAME, EXPERIMENT_ACCESSION, ExperimentFileType.EXPERIMENT_METADATA.getId());

        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_URL, EXPERIMENT_ACCESSION)
                .param("fileType", ExperimentFileType.EXPERIMENT_METADATA.getId()))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedArchiveName))
                .andExpect(content().contentType("application/zip"));
    }

    @Test
    void downloadArchiveForInvalidExperimentAccession() throws Exception {
        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_URL, "FOO")
                .param("fileType", ExperimentFileType.EXPERIMENT_DESIGN.getId()))
                .andExpect(status().is(404))
                .andExpect(view().name("error-page"));
    }

    @Test
    void downloadArchiveForInvalidExperimentFileType() throws Exception {
        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_URL, EXPERIMENT_ACCESSION)
                .param("fileType", "foo"))
                .andExpect(status().is(404))
                .andExpect(view().name("error-page"));
    }

    @Test
    void downloadArchiveForValidExperimentAccessions() throws Exception {
        var result = this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_LIST_URL)
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(1)));

        var expectedArchiveName =
                MessageFormat.format(
                        ARCHIVE_NAME, EXPERIMENT_ACCESSION_LIST.size(), "experiment");

        var contentBytes = result.andReturn().getResponse().getContentAsByteArray();
        var zipInputStream = new ZipInputStream(new ByteArrayInputStream(contentBytes));

        var contentFileNames = ImmutableList.<String>builder();
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            contentFileNames.add(Paths.get(entry.getName()).getFileName().toString());
        }

        assertThat(getSourceValidFileNames().containsAll(contentFileNames.build())).isTrue();
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedArchiveName));
    }

    @Test
    void downloadArchiveForInvalidExperimentAccessions() throws Exception {
        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_LIST_URL)
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(1)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().string(""));
    }

    @Test
    void downloadArchiveForMixedValidAndInvalidExperimentAccessions() throws Exception {
        var result = this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_LIST_URL)
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(1))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(1)));

        var expectedArchiveName =
                MessageFormat.format(
                        ARCHIVE_NAME, EXPERIMENT_ACCESSION_LIST.size(), "experiment");

        var contentBytes = result.andReturn().getResponse().getContentAsByteArray();
        var zipInputStream = new ZipInputStream(new ByteArrayInputStream(contentBytes));

        var contentFileNames = ImmutableList.<String>builder();
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            contentFileNames.add(Paths.get(entry.getName()).getFileName().toString());
        }

        assertThat(getSourceValidFileNames().containsAll(contentFileNames.build())).isTrue();
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedArchiveName))
                .andExpect(content().contentType("application/zip"));
    }

    private ImmutableList<String> getSourceValidFileNames() {
        var paths = ImmutableList.<Path>builder()
                .addAll(experimentFileLocationService.getFilePathsForArchive(
                        EXPERIMENT_ACCESSION_LIST.get(0), ExperimentFileType.QUANTIFICATION_RAW))
                .addAll(experimentFileLocationService.getFilePathsForArchive(
                        EXPERIMENT_ACCESSION_LIST.get(0), ExperimentFileType.NORMALISED))
                .add(experimentFileLocationService.getFilePath(
                        EXPERIMENT_ACCESSION_LIST.get(0), ExperimentFileType.EXPERIMENT_DESIGN))
                .addAll(experimentFileLocationService.getFilePathsForArchive(
                        EXPERIMENT_ACCESSION_LIST.get(1), ExperimentFileType.QUANTIFICATION_RAW))
                .addAll(experimentFileLocationService.getFilePathsForArchive(
                        EXPERIMENT_ACCESSION_LIST.get(1), ExperimentFileType.NORMALISED))
                .add(experimentFileLocationService.getFilePath(
                        EXPERIMENT_ACCESSION_LIST.get(1), ExperimentFileType.EXPERIMENT_DESIGN))
                .build();

        return paths.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(toImmutableList());
    }

    @Test
    void checkArchiveForValidExperimentAccession() throws Exception {
        this.mockMvc.perform(get(ARCHIVE_CHECK_LIST_URL)
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(1)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.invalidFiles", isA(Map.class)))
                .andExpect(jsonPath("$.invalidFiles", hasKey(EXPERIMENT_ACCESSION_LIST.get(0))))
                .andExpect(jsonPath("$.invalidFiles", hasKey(EXPERIMENT_ACCESSION_LIST.get(1))));

    }
}
