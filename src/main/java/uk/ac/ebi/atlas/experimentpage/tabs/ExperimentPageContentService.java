package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.download.ExperimentFileType;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Component
public class ExperimentPageContentService {
    private final ExperimentFileLocationService experimentFileLocationService;
    private final DataFileHub dataFileHub;
    private final TSnePlotSettingsService tsnePlotSettingsService;
    private final CellMetadataService cellMetadataService;

    public ExperimentPageContentService(ExperimentFileLocationService experimentFileLocationService,
                                        DataFileHub dataFileHub,
                                        TSnePlotSettingsService tsnePlotSettingsService,
                                        CellMetadataService cellMetadataService) {
        this.experimentFileLocationService = experimentFileLocationService;
        this.dataFileHub = dataFileHub;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
        this.cellMetadataService = cellMetadataService;
    }

    public JsonObject getTsnePlotData(String experimentAccession) {
        var result = new JsonObject();

        result.add(
                "ks",
                GSON.toJsonTree(tsnePlotSettingsService.getAvailableKs(experimentAccession)));
        result.add(
                "ksWithMarkerGenes",
                GSON.toJsonTree(tsnePlotSettingsService.getKsWithMarkerGenes(experimentAccession)));

        tsnePlotSettingsService.getExpectedClusters(experimentAccession)
                .ifPresent(value -> result.addProperty("selectedK", value));

        result.add("perplexities", getPerplexities(experimentAccession));
        result.add("metadata", getMetadata(experimentAccession));

        var units = new JsonArray();
        units.add("CPM");
        result.add("units", units);

        result.addProperty("suggesterEndpoint", "json/suggestions");

        return result;
    }

    public JsonObject getExperimentDesign(String experimentAccession,
                                          JsonObject experimentDesignTableAsJson,
                                          String accessKey) {
        var result = new JsonObject();

        result.add("table", experimentDesignTableAsJson);

        var fileUri =
                experimentFileLocationService.getFileUri(
                        experimentAccession, ExperimentFileType.EXPERIMENT_DESIGN, accessKey).toString();

        result.addProperty("downloadUrl", fileUri);

        return result;
    }

    public JsonArray getDownloads(String experimentAccession, String accessKey) {
        var result = new JsonArray();

        var metadataFiles =
                ImmutableList.of(
                        ExperimentFileType.EXPERIMENT_METADATA,
                        ExperimentFileType.EXPERIMENT_DESIGN);
        var resultFiles =
                ImmutableList.of(
                        ExperimentFileType.CLUSTERING,
                        ExperimentFileType.QUANTIFICATION_FILTERED,
                        ExperimentFileType.MARKER_GENES,
                        ExperimentFileType.NORMALISED,
                        ExperimentFileType.QUANTIFICATION_RAW);

        result.add(getDownloadSection("Metadata files", metadataFiles, experimentAccession, accessKey));
        result.add(getDownloadSection("Result files", resultFiles, experimentAccession, accessKey));

        return result;
    }

    public JsonArray getSupplementaryInformation(String experimentAccession) {
        var supplementaryInformationTabs = new JsonArray();
        if (dataFileHub.getSingleCellExperimentFiles(experimentAccession).softwareUsed.exists()) {
            supplementaryInformationTabs.add(
                    customContentTab(
                            "static-table",
                            "Analysis Methods",
                            "data",
                            getAnalysisMethods(experimentAccession)));
        }

        return supplementaryInformationTabs;
    }

    public JsonArray getAnalysisMethods(String experimentAccession) {
        var result = new JsonArray();

        try (TsvStreamer tsvStreamer =
                     dataFileHub.getSingleCellExperimentFiles(experimentAccession).softwareUsed.get()) {
            result = GSON.toJsonTree(tsvStreamer.get().collect(Collectors.toList())).getAsJsonArray();
        }

        return result;
    }

    public JsonArray getPerplexities(String experimentAccession) {
        var perplexityArray = new JsonArray();
        tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).forEach(perplexityArray::add);
        return perplexityArray;
    }

    public JsonArray getMetadata(String experimentAccession) {
        var metadataArray = new JsonArray();
        cellMetadataService.getMetadataTypes(experimentAccession)
                .stream()
                .map(x -> ImmutableMap.of("value", x, "label", StringUtil.snakeCaseToDisplayName(x)))
                .collect(Collectors.toSet())
                .forEach(x -> metadataArray.add(GSON.toJsonTree(x)));
        return metadataArray;
    }

    private JsonObject getExperimentFile(ExperimentFileType experimentFileType,
                                         String experimentAccession,
                                         String accessKey) {
        var url =
                experimentFileLocationService
                        .getFileUri(experimentAccession, experimentFileType, accessKey)
                        .toString();

        var result = new JsonObject();

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
        var section = new JsonObject();
        section.addProperty("title", sectionName);

        var files = new JsonArray();
        experimentFileTypes.forEach(file -> files.add(getExperimentFile(file, experimentAccession, accessKey)));

        section.add("files", files);

        return section;
    }

    private JsonObject customContentTab(String tabType, String name, String onlyPropName, JsonElement value) {
        var props =  new JsonObject();
        props.add(onlyPropName, value);
        return customContentTab(tabType, name, props);
    }

    private JsonObject customContentTab(String tabType, String name, JsonObject props) {
        var result = new JsonObject();
        result.addProperty("type", tabType);
        result.addProperty("name", name);
        result.add("props", props);
        return result;
    }
}
