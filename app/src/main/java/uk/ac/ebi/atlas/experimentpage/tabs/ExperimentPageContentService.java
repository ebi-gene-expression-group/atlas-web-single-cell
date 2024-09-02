package uk.ac.ebi.atlas.experimentpage.tabs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.download.ExperimentFileLocationService;
import uk.ac.ebi.atlas.download.ExperimentFileType;
import uk.ac.ebi.atlas.experimentpage.ExternallyAvailableContentService;
import uk.ac.ebi.atlas.experimentpage.cellplot.CellPlotService;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.search.OntologyAccessionsSearchService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Service
public class ExperimentPageContentService {
    private final ExperimentFileLocationService experimentFileLocationService;
    private final DataFileHub dataFileHub;
    private final TSnePlotSettingsService tsnePlotSettingsService;
    private final CellMetadataService cellMetadataService;
    private final OntologyAccessionsSearchService ontologyAccessionsSearchService;
    private final ExperimentTrader experimentTrader;
    private final CellPlotService cellPlotService;

    final static ImmutableSet<String> EXPERIMENTS_WITH_NO_ANATOMOGRAM = ImmutableSet.of(
            "E-GEOD-130473", "E-HCAD-8", "E-MTAB-6653", "E-GEOD-86618", "E-CURD-11", "E-MTAB-6308",
            "E-HCAD-10", "E-CURD-10", "E-GEOD-114530", "E-MTAB-7407", "E-MTAB-9067", "E-CURD-126",
            "E-ANND-2","E-ANND-3","E-ANND-4","E-ANND-5");

    public ExperimentPageContentService(ExperimentFileLocationService experimentFileLocationService,
                                        DataFileHub dataFileHub,
                                        TSnePlotSettingsService tsnePlotSettingsService,
                                        CellMetadataService cellMetadataService,
                                        OntologyAccessionsSearchService ontologyAccessionsSearchService,
                                        ExperimentTrader experimentTrader,
                                        CellPlotService cellPlotService) {
        this.experimentFileLocationService = experimentFileLocationService;
        this.dataFileHub = dataFileHub;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
        this.cellMetadataService = cellMetadataService;
        this.ontologyAccessionsSearchService = ontologyAccessionsSearchService;
        this.experimentTrader = experimentTrader;
        this.cellPlotService = cellPlotService;
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

        result.add("plotTypesAndOptions",
                GSON.toJsonTree(tsnePlotSettingsService.getAvailablePlotTypesAndPlotOptions(experimentAccession)));

        result.add("defaultPlotMethodAndParameterisation",
                GSON.toJsonTree(fetchDefaultPlotMethodAndParameterisation(experimentAccession)));

        result.add("metadata", getMetadata(experimentAccession));

        var units = new JsonArray();
        units.add("CPM");
        result.add("units", units);

        result.addProperty("suggesterEndpoint", "json/suggestions");

        result.add(
                "anatomogram",
                EXPERIMENTS_WITH_NO_ANATOMOGRAM.contains(experimentAccession) ?
                        new JsonObject() :
                        GSON.toJsonTree(
                                ontologyAccessionsSearchService
                                        .searchAvailableAnnotationsForOrganAnatomogram(experimentAccession)));

        return result;
    }

    public JsonArray getDownloads(String experimentAccession, String accessKey) {
        var result = new JsonArray();
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        var technologyType = experiment.getTechnologyType();

        var metadataFiles =
                ImmutableList.of(
                        ExperimentFileType.EXPERIMENT_METADATA,
                        ExperimentFileType.EXPERIMENT_DESIGN);

        var resultFiles = isSmartExperiment(technologyType) ?
                ImmutableList.of(
                        ExperimentFileType.CLUSTERING,
                        ExperimentFileType.QUANTIFICATION_FILTERED,
                        ExperimentFileType.MARKER_GENES,
                        ExperimentFileType.NORMALISED,
                        ExperimentFileType.QUANTIFICATION_RAW) :
                ImmutableList.of(
                        ExperimentFileType.CLUSTERING,
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

            supplementaryInformationTabs.add(
                    customContentTab(
                            "resources",
                            "Resources",
                            "url",
                            new JsonPrimitive(
                                    ExternallyAvailableContentService.listResourcesUrl(
                                            experimentAccession,
                                            "",
                                            ExternallyAvailableContent.ContentType.SUPPLEMENTARY_INFORMATION)))
            );
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
        cellMetadataService
                .getMetadataTypes(experimentAccession)
                .stream()
                .filter(type -> cellMetadataService
                        .getMetadataValuesForGivenType(experimentAccession, type)
                        .values()
                        .stream()
                        .distinct()
                        .count() > 1)   // show only metadata types with more than 1 value
                .map(x -> ImmutableMap.of("value", x, "label", StringUtil.snakeCaseToDisplayName(x)))
                .collect(Collectors.toSet())
                .forEach(metadata -> metadataArray.add(GSON.toJsonTree(metadata)));

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
        result.addProperty("description", experimentFileType.getDescription());
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
        var props = new JsonObject();
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

    // Smart-Seq-like experiments will contain the substring “smart” in their technology types
    private static boolean isSmartExperiment(Collection<String> technologyType) {
        return technologyType.stream()
                .anyMatch(type -> type.toLowerCase().matches("smart" + "-(?:.*)"));
    }

    public ImmutableMap fetchDefaultPlotMethodAndParameterisation(String experimentAccession){
        return cellPlotService.fetchDefaultPlotMethodWithParameterisation(experimentAccession);
    }
}
