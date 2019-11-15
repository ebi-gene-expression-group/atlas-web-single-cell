package uk.ac.ebi.atlas.download;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
public class FileDownloadController extends HtmlExceptionHandlingController {
    private final ExperimentFileLocationService experimentFileLocationService;
    private final ExperimentTrader experimentTrader;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);

    public FileDownloadController(ExperimentFileLocationService experimentFileLocationService,
                                  ExperimentTrader experimentTrader) {
        this.experimentFileLocationService = experimentFileLocationService;
        this.experimentTrader = experimentTrader;
    }

    @GetMapping(value = "experiment/{experimentAccession}/download")
    public ResponseEntity<FileSystemResource>
    download(@PathVariable String experimentAccession,
             @RequestParam(value = "fileType") String fileTypeId,
             @RequestParam(value = "accessKey", defaultValue = "") String accessKey) {

        Experiment experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        var file =
                experimentFileLocationService.getFilePath(
                        experiment.getAccession(), ExperimentFileType.fromId(fileTypeId)).toFile();
        var resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment;filename=" + file.getName())
                .contentType(MediaType.TEXT_PLAIN).contentLength(file.length())
                .body(resource);
    }

    @GetMapping(value = "experiment/{experimentAccession}/download/zip",
                produces = "application/zip")
    public void
    downloadArchive(HttpServletResponse response,
                    @PathVariable String experimentAccession,
                    @RequestParam(value = "fileType") String fileTypeId) throws IOException {

        var experiment = experimentTrader.getPublicExperiment(experimentAccession);
        var paths =
                experimentFileLocationService
                        .getFilePathsForArchive(experiment.getAccession(), ExperimentFileType.fromId(fileTypeId));

        var archiveName = experimentAccession + "-" + fileTypeId + "-files.zip";
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + archiveName);
        response.setContentType("application/zip");
        var zipOutputStream = new ZipOutputStream(response.getOutputStream());

        for (var path : paths) {
            var file = path.toFile();

            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);

            IOUtils.copy(fileInputStream, zipOutputStream);

            fileInputStream.close();
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
    }

    @GetMapping(value = "experiments/download/zip",
                produces = "application/zip")
    public void
    downloadMultipleExperimentsArchive(HttpServletResponse response,
                                       @RequestParam(value = "accession", defaultValue = "") List<String> accessions)
            throws IOException {

        var experiments = new ArrayList<Experiment>();
        for (var accession : accessions) {
            try {
                experiments.add(experimentTrader.getPublicExperiment(accession));
            } catch (Exception e) {
                LOGGER.debug("Invalid experiment accession: {}", accession);
            }
        }

        if (!experiments.isEmpty()) {
            var archiveName = experiments.size() + "-experiment-files.zip";
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + archiveName);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/zip");
            var zipOutputStream = new ZipOutputStream(response.getOutputStream());

            for (var experiment : experiments) {
                var paths = ImmutableList.<Path>builder()
                        .addAll(experimentFileLocationService.getFilePathsForArchive(
                                experiment.getAccession(), ExperimentFileType.QUANTIFICATION_RAW))
                        .addAll(experimentFileLocationService.getFilePathsForArchive(
                                experiment.getAccession(), ExperimentFileType.NORMALISED))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.EXPERIMENT_DESIGN))
                        .build();

                for (var path : paths) {
                    var file = path.toFile();
                    if (file.exists()) {
                        zipOutputStream.putNextEntry(new ZipEntry(experiment.getAccession() + "/" + file.getName()));
                        FileInputStream fileInputStream = new FileInputStream(file);

                        IOUtils.copy(fileInputStream, zipOutputStream);
                        fileInputStream.close();
                        zipOutputStream.closeEntry();
                    }
                }
            }
            zipOutputStream.close();
        }
    }

    @GetMapping(value = "json/experiments/download/zip/check",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String validateExperimentsFiles(@RequestParam(value = "accession", defaultValue = "") List<String> accessions)
    {
        var experiments = ImmutableSet.<Experiment>builder();

        accessions.forEach(accession -> {
            try {
                experiments.add(experimentTrader.getPublicExperiment(accession));
            } catch (Exception e) {
                LOGGER.debug("Invalid experiment accession: {}", accession);
            }
        });

        var fileTypeCheckList = ImmutableList.of(
                ExperimentFileType.QUANTIFICATION_RAW,
                ExperimentFileType.NORMALISED,
                ExperimentFileType.EXPERIMENT_DESIGN);

        var filePaths = experiments.build().stream()
                .distinct()
                .collect(toImmutableMap(
                        Experiment::getAccession,
                        experiment -> fileTypeCheckList.stream()
                                .map(fileType -> fileType == ExperimentFileType.EXPERIMENT_DESIGN ?
                                        ImmutableList.of(experimentFileLocationService
                                                .getFilePath(experiment.getAccession(), fileType)) :
                                        experimentFileLocationService
                                                .getFilePathsForArchive(experiment.getAccession(), fileType))
                                .flatMap(List::stream)
                                .collect(ImmutableList.toImmutableList())
                ));

        var invalidFilesList = filePaths.entrySet()
                .stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        experiment -> experiment.getValue()
                                .stream()
                                .filter(path -> path.toFile().exists() == false)
                                .map(path -> path.getFileName().toString())
                                .collect(toImmutableList())
                ));

        if(!invalidFilesList.isEmpty()){
            LOGGER.debug("Invalid experiment files: {}", invalidFilesList);
        }

        return GSON.toJson(Collections.singletonMap("invalidFiles", invalidFilesList));
    }
}
