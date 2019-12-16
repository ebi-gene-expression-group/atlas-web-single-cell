package uk.ac.ebi.atlas.home;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.home.species.SpeciesSummary;
import uk.ac.ebi.atlas.home.species.SpeciesSummarySerializer;
import uk.ac.ebi.atlas.home.species.SpeciesSummaryService;

import java.util.Map;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

@RestController
public class JsonSpeciesSummaryController extends JsonExceptionHandlingController {
    private final SpeciesSummaryService speciesSummaryService;
    private final SpeciesSummarySerializer speciesSummarySerializer;

    public JsonSpeciesSummaryController(SpeciesSummaryService speciesSummaryService,
                                        SpeciesSummarySerializer speciesSummarySerializer) {
        this.speciesSummaryService = speciesSummaryService;
        this.speciesSummarySerializer = speciesSummarySerializer;
    }

    @Cacheable("speciesSummary")
    @GetMapping(value = "/json/species-summary",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getSpeciesSummaryGroupedByKingdom() {
        return speciesSummarySerializer.serialize(
                // Replace card models with baseline/differential experiments with only experiments
                speciesSummaryService.getReferenceSpeciesSummariesGroupedByKingdom().entrySet().stream()
                        .collect(toImmutableMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(summary ->
                                                        SpeciesSummary.create(
                                                                summary.getSpecies(),
                                                                summary.getKingdom(),
                                                                summary.getBaselineExperiments() +
                                                                        summary.getDifferentialExperiments()))
                                                .collect(toImmutableList()))));
    }
}
