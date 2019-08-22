package uk.ac.ebi.atlas.download;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class FileDownloadController extends HtmlExceptionHandlingController {
    private final ExperimentFileLocationService experimentFileLocationService;
    private final ExperimentTrader experimentTrader;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);

    @Inject
    public FileDownloadController(ExperimentFileLocationService experimentFileLocationService,
                                  ExperimentTrader experimentTrader) {
        this.experimentFileLocationService = experimentFileLocationService;
        this.experimentTrader = experimentTrader;
    }

    @RequestMapping(value = "experiment/{experimentAccession}/download",
                    method = RequestMethod.GET)
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

    @RequestMapping(value = "experiment/{experimentAccession}/download/zip",
                    method = RequestMethod.GET,
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

    @RequestMapping(value = "experiments/download/zip",
                    method = RequestMethod.GET,
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
}
