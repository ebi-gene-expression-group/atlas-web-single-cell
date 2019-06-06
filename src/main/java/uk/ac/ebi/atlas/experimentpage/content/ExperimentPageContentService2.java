package uk.ac.ebi.atlas.experimentpage.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.ExperimentPageContentService;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesignTable;
import uk.ac.ebi.atlas.model.experiment.sample.Cell;
import uk.ac.ebi.atlas.resource.DataFileHub;

@Component
public class ExperimentPageContentService2 {
    private final DataFileHub dataFileHub;
    private final ExperimentPageContentService experimentPageContentService;

    public ExperimentPageContentService2(DataFileHub dataFileHub,
                                         ExperimentPageContentService experimentPageContentService) {
        this.dataFileHub = dataFileHub;
        this.experimentPageContentService = experimentPageContentService;
    }

    @Cacheable(value = "experimentContent")
    public JsonObject experimentPageContentForExperiment(final Experiment<Cell> experiment, final String accessKey) {
        var result = new JsonObject();

        result.addProperty("experimentAccession", experiment.getAccession());
        result.addProperty("accessKey", accessKey);
        result.addProperty("species", experiment.getSpecies().getReferenceName());
        result.addProperty("disclaimer", experiment.getDisclaimer());

        var availableTabs = new JsonArray();

        availableTabs.add(
                customContentTab(
                        "results",
                        "Results",
                        experimentPageContentService.getTsnePlotData(experiment.getAccession())));

        if (dataFileHub.getExperimentFiles(experiment.getAccession()).experimentDesign.exists()) {
            availableTabs.add(
                    customContentTab(
                            "experiment-design",
                            "Experiment Design",
                            experimentPageContentService.getExperimentDesign(
                                    experiment.getAccession(),
                                    new ExperimentDesignTable(experiment).asJson(),
                                    accessKey))
            );
        }

        availableTabs.add(
                customContentTab(
                        "supplementary-information",
                        "Supplementary Information",
                        "sections",
                        supplementaryInformationTabs(experiment))
        );

        availableTabs.add(
                customContentTab(
                        "downloads",
                        "Downloads",
                        "data",
                        experimentPageContentService.getDownloads(experiment.getAccession(), accessKey))
        );

        result.add("tabs", availableTabs);

        return result;
    }

    private JsonArray supplementaryInformationTabs(final Experiment experiment) {
        var supplementaryInformationTabs = new JsonArray();
        if (dataFileHub.getSingleCellExperimentFiles(experiment.getAccession()).softwareUsed.exists()) {
                supplementaryInformationTabs.add(
                        customContentTab(
                                "static-table",
                                "Analysis Methods",
                                "data",
                                experimentPageContentService.getAnalysisMethods(experiment.getAccession())));
            }

        return supplementaryInformationTabs;
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
