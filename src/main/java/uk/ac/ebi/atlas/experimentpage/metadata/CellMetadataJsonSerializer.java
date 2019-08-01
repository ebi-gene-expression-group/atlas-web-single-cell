package uk.ac.ebi.atlas.experimentpage.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;
import uk.ac.ebi.atlas.utils.StringUtil;

@Component
public class CellMetadataJsonSerializer {
    private final ExperimentTrader experimentTrader;
    private final CellMetadataService cellMetadataService;

    public CellMetadataJsonSerializer(ScxaExperimentTrader experimentTrader, CellMetadataService cellMetadataService) {
        this.experimentTrader = experimentTrader;
        this.cellMetadataService = cellMetadataService;
    }

    public String getCellMetadata(String experimentAccession, String cellId, String accessKey) {
        Experiment experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        JsonArray result = new JsonArray();

        cellMetadataService.getMetadataValues(experiment.getAccession(), cellId)
                .forEach((metadataName, metadataValue) ->
                        result.add(createMetadataJson(metadataName, metadataValue)));

        return result.toString();
    }

    private JsonObject createMetadataJson(String name, String value) {
        JsonObject result = new JsonObject();

        result.addProperty("displayName", StringUtil.snakeCaseToDisplayName(name));
        result.addProperty("value", value);

        return  result;
    }

}
