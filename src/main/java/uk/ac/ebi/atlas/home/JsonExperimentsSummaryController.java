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
import static uk.ac.ebi.atlas.utils.UrlHelpers.*;

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

    private static ImmutableList<CardModel> featuredExperimentsCards() {
        return ImmutableList.of(
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("hca"),
                        Pair.of(Optional.of("Human Cell Atlas"), Optional.of(getCustomUrl("/hca-landing-page.html"))),
                        ImmutableList.of()),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("cz-biohub"),
                        getExperimentLink("E-ENAD-15"),
                        ImmutableList.of(
                                getExperimentLink("Tabula Muris", "E-ENAD-15"))),
                CardModel.create(
                        IMAGE,
                        getExperimentsSummaryImageUrl("malaria-cell-atlas"),
                        getExperimentLink("E-CURD-2"),
                        ImmutableList.of(
                                getExperimentLink("Malaria Cell Atlas – Malaria parasites", "E-CURD-2"))));
    }
}
