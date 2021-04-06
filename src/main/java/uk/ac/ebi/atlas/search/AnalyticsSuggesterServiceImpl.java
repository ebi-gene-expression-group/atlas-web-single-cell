package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import org.apache.solr.client.solrj.response.Suggestion;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class AnalyticsSuggesterServiceImpl implements AnalyticsSuggesterService {

    private AnalyticsSuggesterDao analyticsSuggesterDao;
    private final SpeciesFactory speciesFactory;
    private static final Function<Suggestion, Map<String, String>>
            SUGGESTION_TO_MAP =
            suggestion -> ImmutableMap.of("term", suggestion.getTerm(), "category", suggestion.getPayload());

    public AnalyticsSuggesterServiceImpl(AnalyticsSuggesterDao analyticsSuggesterDao,SpeciesFactory speciesFactory){
        this.analyticsSuggesterDao = analyticsSuggesterDao;
        this.speciesFactory = speciesFactory;
    }

    @Override
    public Stream<Map<String, String>> fetchOntologyAnnotationSuggestions(String query, String... species) {
        Species[] speciesArray = Arrays.stream(species).map(speciesFactory::create).toArray(Species[]::new);
        var response = analyticsSuggesterDao.fetchOntologyAnnotationSuggestions(query,100, speciesArray);

        System.out.println("suggestions:"+response.map(SUGGESTION_TO_MAP).count());

        return response.map(SUGGESTION_TO_MAP);
    }
}
