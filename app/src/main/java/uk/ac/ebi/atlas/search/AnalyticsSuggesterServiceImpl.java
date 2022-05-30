package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import org.apache.solr.client.solrj.response.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Service
public class AnalyticsSuggesterServiceImpl implements AnalyticsSuggesterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSuggesterServiceImpl.class);

    // Remember that suggestions are distinct()’ed, so this value is an upper bound
    public static final int DEFAULT_MAX_NUMBER_OF_SUGGESTIONS = 10;
    private final AnalyticsSuggesterDao analyticsSuggesterDao;
    private final SpeciesFactory speciesFactory;

    private static final Function<Suggestion, Map<String, String>> SUGGESTION_TO_MAP =
            suggestion -> ImmutableMap.of("term", suggestion.getTerm(), "category", "metadata");

    public AnalyticsSuggesterServiceImpl(AnalyticsSuggesterDao analyticsSuggesterDao, SpeciesFactory speciesFactory) {
        this.analyticsSuggesterDao = analyticsSuggesterDao;
        this.speciesFactory = speciesFactory;
    }

    @Override
    public Stream<Map<String, String>> fetchMetadataSuggestions(String query, String... species) {
        var speciesNames =
                Arrays.stream(species)
                        .map(speciesFactory::create)
                        .map(Species::getName)
                        .collect(toImmutableSet());
        var response = analyticsSuggesterDao.fetchMetadataSuggestions(query, DEFAULT_MAX_NUMBER_OF_SUGGESTIONS);

        return response
                .filter(suggestion -> speciesNames.isEmpty() || speciesNames.contains(suggestion.getPayload()))
                .map(SUGGESTION_TO_MAP);
    }
}
