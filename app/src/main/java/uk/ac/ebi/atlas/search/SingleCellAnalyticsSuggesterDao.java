package uk.ac.ebi.atlas.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Suggestion;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.species.Species;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Repository
public class SingleCellAnalyticsSuggesterDao implements AnalyticsSuggesterDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCellAnalyticsSuggesterDao.class);

    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    private static final String[] DICTIONARIES = {"ontologyAnnotationSuggester", "ontologyAnnotationAncestorSuggester",
                    "ontologyAnnotationParentSuggester", "ontologyAnnotationSynonymSuggester",
                    "ontologyAnnotationChildSuggester"};

    public SingleCellAnalyticsSuggesterDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Override
    public Stream<Suggestion> fetchOntologyAnnotationSuggestions(String query, int limit, Species... species) {
        return fetchOntologySuggestions(query, limit, species);
    }

    private Stream<Suggestion> fetchOntologySuggestions(String query, int limit, Species... species) {

        // We want the user to go beyond one keystroke to get some suggestions
        if (query.length() < 2) {
            return Stream.empty();
        }

        var compareByWeightLengthAlphabetical = Comparator.comparingLong(Suggestion::getWeight).reversed()
                .thenComparingInt(suggestion -> suggestion.getTerm().length())
                .thenComparing(Suggestion::getTerm);

        var solrQuery = new SolrQuery();
        solrQuery.setRequestHandler("/suggest")
                .setParam("suggest.dictionary", DICTIONARIES)
                .setParam("suggest.q", query)
                // We raise suggest.count to a high enough value to get exact matches (the default is 100)
                .setParam("suggest.count", "750").setParam("suggest.cfq",
                Arrays.stream(species).map(Species::getEnsemblName).collect(joining(" ")));
        return fetchAnalyticsSuggestions(solrQuery,compareByWeightLengthAlphabetical,limit);
    }

    private Stream<Suggestion> fetchAnalyticsSuggestions(SolrQuery solrQuery,Comparator<Suggestion> compareByWeightLengthAlphabetical,int limit){
        try {
            return singleCellAnalyticsCollectionProxy.solrClient.query("scxa-analytics", solrQuery)
                    .getSuggesterResponse()
                    .getSuggestions().values().stream().flatMap(List::stream)
                    .distinct().sorted(compareByWeightLengthAlphabetical).limit(limit);
        } catch (SolrServerException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SolrException(SolrException.ErrorCode.UNKNOWN, e);
        }
    }
}
