package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityCardModelFactory;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.species.SpeciesInferrer;

import javax.inject.Inject;

import static uk.ac.ebi.atlas.bioentity.properties.BioEntityCardProperties.BIOENTITY_PROPERTY_NAMES;
import static uk.ac.ebi.atlas.solr.BioentityPropertyName.SYMBOL;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonBioentityInformationController extends JsonExceptionHandlingController {
    private final BioEntityPropertyDao bioEntityPropertyDao;
    private final BioEntityCardModelFactory bioEntityCardModelFactory;
    private final SpeciesInferrer speciesInferrer;

    @Inject
    public JsonBioentityInformationController(BioEntityPropertyDao bioEntityPropertyDao,
                                              BioEntityCardModelFactory bioEntityCardModelFactory,
                                              SpeciesInferrer speciesInferrer) {
        this.bioEntityPropertyDao = bioEntityPropertyDao;
        this.bioEntityCardModelFactory = bioEntityCardModelFactory;
        this.speciesInferrer = speciesInferrer;
    }

    @GetMapping(value = "/json/bioentity-information/{geneId:.+}",  // Don't truncate ".value" at the end of IDs
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String jsonBioentityInformation(@PathVariable String geneId) {
        var species = speciesInferrer.inferSpeciesForGeneQuery(SemanticQuery.create(geneId));
        var propertyValues = bioEntityPropertyDao.fetchGenePageProperties(geneId);
        var geneName = String.join("/", bioEntityPropertyDao.fetchPropertyValuesForGeneId(geneId, SYMBOL));

        var model =
                bioEntityCardModelFactory.modelAttributes(
                        geneId,
                        species,
                        BIOENTITY_PROPERTY_NAMES,
                        geneName,
                        propertyValues);

        var jsonArray = GSON.fromJson(model.get("bioentityProperties").toString(), JsonArray.class);
        jsonArray.add(
                GSON.toJsonTree(
                        ImmutableMap.of(
                                "type", "expression_atlas",
                                "name", "Expression Atlas",
                                "values", expressionAtlasBioentityPropertyValues(geneId))));

        return GSON.toJson(ImmutableMap.of("bioentityProperties", jsonArray));
    }

    private static ImmutableList<ImmutableMap<String, ?>> expressionAtlasBioentityPropertyValues(final String geneId) {
        return ImmutableList.of(
                ImmutableMap.of(
                    "text", geneId,
                    "url", "https://www.ebi.ac.uk/gxa/genes/" + geneId,
                    "relevance", 0));
    }
}
