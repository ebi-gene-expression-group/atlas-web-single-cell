package uk.ac.ebi.atlas.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public SingleCellAnalyticsSuggesterDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy = solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Override
    public Stream<Suggestion> fetchOntologyAnnotationSuggestions(String query, int limit, Species... species) {
        return fetchOntologySuggestions(query,limit,species);
    }

    @Override
    public Stream<Suggestion> fetchOntologySuggestions(String query, int limit, Species... species) {
        // We want the user to go beyond one keystroke to get some suggestions
        if (query.length() < 2) {
            return Stream.empty();
        }

        var compareByWeightLengthAlphabetical =
                Comparator
                        .comparingLong(Suggestion::getWeight).reversed()
                        .thenComparingInt(suggestion -> suggestion.getTerm().length())
                        .thenComparing(Suggestion::getTerm);
        //We are using multiple dictionaries to fetch various ontology annotation label suggestions
        //These are the ontology annotation attributes configures on Solr to fetch suggestions:
        var dictionaries = new String[] {
                "ontologyAnnotationSuggester", "ontologyAnnotationAncestorSuggester", "ontologyAnnotationParentSuggester",
                "ontologyAnnotationSynonymSuggester", "ontologyAnnotationChildSuggester" };

        var solrQuery = buildSuggesterRequestHandler(dictionaries, query, species);
        return getSuggestionStream(limit, compareByWeightLengthAlphabetical, solrQuery);
    }

    @Nullable
    private Stream<Suggestion> getSuggestionStream(int limit,
                                                   Comparator<Suggestion> compareByWeightLengthAlphabetical,
                                                   SolrQuery solrQuery) {
        Stream<Suggestion> suggestionStream = null;
        try {
            suggestionStream = singleCellAnalyticsCollectionProxy.solrClient
                    .query("scxa-analytics", solrQuery).getSuggesterResponse()
                    .getSuggestions().values().stream()
                    .flatMap(List::stream)
                    // The current implementation considers symbols Aspm and ASPM two different suggestions. I dont’t know
                    // if that’s good or bad because I don’t know if to a biologist it’s meaningful (I guess not). If we
                    // change it it should be reflected in a story.
                    .distinct()
                    .sorted(compareByWeightLengthAlphabetical)
                    .limit(limit);
        } catch (SolrServerException e) {
           LOGGER.debug("SolrServerException: {}",e.getMessage());
           LOGGER.trace("SolrServerException: {}",e.getStackTrace());
        } catch (IOException e) {
            LOGGER.debug("IOException: {}",e.getMessage());
            LOGGER.trace("IOException: {}",e.getStackTrace());
        }
        catch (Exception e) {
            LOGGER.debug("Exception: {}",e.getMessage());
            LOGGER.trace("Exception: {}",e.getStackTrace());
        }
        return suggestionStream;
    }

    @NotNull
    private SolrQuery buildSuggesterRequestHandler(String[] dictionaries, String query, Species[] species) {
        var solrQuery = new SolrQuery();
        solrQuery.setRequestHandler("/suggest")
                //'suggest.build=true' required to build dictionaries for search.
                // For multiple dictionaries it is a mandatory field
                .setParam("suggest.build",Boolean.TRUE)
                .setParam("suggest.dictionary", dictionaries)
                .setParam("suggest.q", query)
                // We raise suggest.count to a high enough value to get exact matches (the default is 100)
                .setParam("suggest.count", "750")
                .setParam(
                        "suggest.cfq",
                        Arrays.stream(species)
                                .map(Species::getEnsemblName)
                                .collect(joining(" ")));
        return solrQuery;
    }
}
