package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.model.card.CardModel;
import uk.ac.ebi.atlas.model.card.CardModelAdapter;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static uk.ac.ebi.atlas.model.card.CardIconType.IMAGE;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getCustomUrl;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getExperimentCollectionLink;
import static uk.ac.ebi.atlas.utils.UrlHelpers.getExperimentsSummaryImageUrl;

@RestController
public class JsonExperimentsSummaryController extends JsonExceptionHandlingController {
    private final LatestExperimentsService latestExperimentsService;

    public JsonExperimentsSummaryController(LatestExperimentsService latestExperimentsService) {
        this.latestExperimentsService = latestExperimentsService;
    }

    @GetMapping(value = "/json/experiments-summary",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getLatestExperiments() {
        return GSON.toJson(
                ImmutableMap.of(
                        "latestExperiments",
                        latestExperimentsService.fetchLatestExperimentsAttributes().get("latestExperiments"),
                        "featuredExperiments",
                        featuredExperimentsCards().stream()
                                .map(CardModelAdapter::serialize)
                                .collect(toImmutableList())));
    }

    private ImmutableList<CardModel> featuredExperimentsCards() {
        return ImmutableList.of(
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("hca"),
                        Pair.of(Optional.of("Human Cell Atlas"), Optional.of(getCustomUrl("/hca-landing-page.html"))),
                        ImmutableList.of()),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("cz-biohub"),
                        getExperimentCollectionLink("Chan Zuckerberg Biohub", "Chan Zuckerberg Biohub"),
                        ImmutableList.of()),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("malaria-cell-atlas"),
                        getExperimentCollectionLink("Malaria Cell Atlas", "Malaria Cell Atlas"),
                        ImmutableList.of()),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("covid19-dp"),
                        getExperimentCollectionLink("COVID-19 Data Portal", "COVID-19"),
                        ImmutableList.of()));
    }
}
