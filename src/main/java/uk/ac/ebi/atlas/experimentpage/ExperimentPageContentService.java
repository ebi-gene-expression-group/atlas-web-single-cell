package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.download.ExperimentFileType;
import uk.ac.ebi.atlas.metadata.CellMetadataService;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Component
public class ExperimentPageContentService {
    private final ExperimentFileLocationService experimentFileLocationService;
    private final DataFileHub dataFileHub;
    private final TsnePlotSettingsService tsnePlotSettingsService;
    private final CellMetadataService cellMetadataService;

    public ExperimentPageContentService(ExperimentFileLocationService experimentFileLocationService,
                                        DataFileHub dataFileHub,
                                        TsnePlotSettingsService tsnePlotSettingsService,
                                        CellMetadataService cellMetadataService) {
        this.experimentFileLocationService = experimentFileLocationService;
        this.dataFileHub = dataFileHub;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
        this.cellMetadataService = cellMetadataService;
    }

    public JsonObject getTsnePlotData(String experimentAccession) {
        JsonObject result = new JsonObject();

        result.add("ks", GSON.toJsonTree(tsnePlotSettingsService.getAvailableKs(experimentAccession)));
        result.add("ksWithMarkerGenes", GSON.toJsonTree(tsnePlotSettingsService.getKsWithMarkerGenes(experimentAccession)));

        tsnePlotSettingsService.getExpectedClusters(experimentAccession)
                .ifPresent(value -> result.addProperty("selectedK", value));

        JsonArray perplexityArray = new JsonArray();
        tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).forEach(perplexityArray::add);
        result.add("perplexities", perplexityArray);

        JsonArray metadataArray = new JsonArray();
        cellMetadataService.getMetadataTypes(experimentAccession)
                .stream()
                .map(x -> ImmutableMap.of("value", x, "label", StringUtil.snakeCaseToDisplayName(x)))
                .collect(Collectors.toSet())
                .forEach(x -> metadataArray.add(GSON.toJsonTree(x)));

        result.add("metadata", metadataArray);

        JsonArray units = new JsonArray();
        units.add("TPM");

        result.add("units", units);

        result.addProperty("suggesterEndpoint", "json/suggestions");

        return result;
    }

    public JsonObject getExperimentDesign(String experimentAccession,
                                          JsonObject experimentDesignTableAsJson,
                                          String accessKey) {
        JsonObject result = new JsonObject();

        result.add("table", experimentDesignTableAsJson);

        String fileUri =
                experimentFileLocationService.getFileUri(
                        experimentAccession, ExperimentFileType.EXPERIMENT_DESIGN, accessKey).toString();

        result.addProperty("downloadUrl", fileUri);

        return result;
    }

    public JsonArray getDownloads(String experimentAccession, String accessKey) {
        JsonArray result = new JsonArray();

        List<ExperimentFileType> metadataFiles =
                Arrays.asList(
                        ExperimentFileType.EXPERIMENT_METADATA,
                        ExperimentFileType.EXPERIMENT_DESIGN);
        List<ExperimentFileType> resultFiles =
                Arrays.asList(
                        ExperimentFileType.CLUSTERING,
                        ExperimentFileType.QUANTIFICATION_FILTERED,
                        ExperimentFileType.MARKER_GENES,
                        ExperimentFileType.NORMALISED,
                        ExperimentFileType.QUANTIFICATION_RAW);

        result.add(getDownloadSection("Metadata files", metadataFiles, experimentAccession, accessKey));
        result.add(getDownloadSection("Result files", resultFiles, experimentAccession, accessKey));

        return result;
    }

    public JsonArray getAnalysisMethods(String experimentAccession) {
        JsonArray result;

        try (TsvStreamer tsvStreamer =
                     dataFileHub.getSingleCellExperimentFiles(experimentAccession).softwareUsed.get()) {
                            result = GSON.toJsonTree(tsvStreamer.get().collect(Collectors.toList())).getAsJsonArray();
        }

        return result;
    }

    private JsonObject getExperimentFile(ExperimentFileType experimentFileType,
                                         String experimentAccession,
                                         String accessKey) {
        String url =
                experimentFileLocationService
                        .getFileUri(experimentAccession, experimentFileType, accessKey)
                        .toString();

        JsonObject result = new JsonObject();

        result.addProperty("url", url);
        result.addProperty("type", experimentFileType.getIconType().getName());
        result.addProperty("description",  experimentFileType.getDescription());
        result.addProperty("isDownload", true);

        return result;
    }

    private JsonObject getDownloadSection(String sectionName,
                                          List<ExperimentFileType> experimentFileTypes,
                                          String experimentAccession,
                                          String accessKey) {
        JsonObject section = new JsonObject();
        section.addProperty("title", sectionName);

        JsonArray files = new JsonArray();
        experimentFileTypes.forEach(file -> files.add(getExperimentFile(file, experimentAccession, accessKey)));

        section.add("files", files);

        return section;
    }
}
