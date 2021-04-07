package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
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
//        Commented out actual code here -fetchOntologySuggestions(query,limit,species) which is contacting Solr suggesters to fetch analytics suggestions
        //        return fetchOntologySuggestions(query,limit,species);
        return fetchAnalyticsDummySuggestions(query,limit,species).values().stream();
    }

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

    /**
     * dummy suggesters for analytics collections
     * @return Stream<Suggestion>
     */
    public ImmutableMap<String, Suggestion> fetchAnalyticsDummySuggestions(String query, int limit, Species... species) {
        return new ImmutableMap.Builder<String, Suggestion>()
                //disease
                .put("diabetes1", new Suggestion("diabetes mellitus", 0, "endocrine pancreas disease"))
                .put("diabetes2", new Suggestion("type II diabetes mellitus", 0, "type II diabetes mellitus"))
                .put("diabetes3", new Suggestion("Diabetes Mellitus, Ketosis Resistant", 0, "T2DM - Type 2 Diabetes mellitus"))
                .put("diabetes4", new Suggestion("T2DM - Type 2 Diabetes mellitus", 0, "T2DM - Type 2 Diabetes mellitus"))
                .put("diabetes6", new Suggestion("Adult-Onset Diabetes Mellitus", 0, "T2DM - Type 2 Diabetes mellitus"))
                .put("diabetes7", new Suggestion("Diabetes Mellitus, Adult-Onsets", 0, "T2DM - Type 2 Diabetes mellitus"))
                .put("diabetes8", new Suggestion("Diabetes, Type 2", 0, "T2DM - Type 2 Diabetes mellitus"))
                //organism part
                .put("skin1", new Suggestion("zone of skin", 0, "zone of skin"))
                .put("skin2", new Suggestion("portion of skin", 0, "portion of skin"))
                .put("skin3", new Suggestion("skin zone", 0, "portion of skin"))
                .put("skin4", new Suggestion("region of skin", 0, "portion of skin"))
                .put("skin5", new Suggestion("skin region", 0, "portion of skin"))
                .put("skin6", new Suggestion("skin", 0, "portion of skin"))
                .put("lymph node1", new Suggestion("lymph node", 0, "lymph node"))
                .put("lymph node2", new Suggestion("mediastinal <b>lymph</b> <b>node</b>", 0, "mesenteric lymph node"))
                .put("lymph node3", new Suggestion("human", 0, "man"))
                .put("lymph node4", new Suggestion("female", 0, "female"))
                .put("lymph node5", new Suggestion("islet of Langerhans", 0, "islet of Langerhans"))
                .put("lymph node6", new Suggestion("collecting specimen from organ postmortem", 0, "collecting specimen from organ postmortem"))
                //disease
                .put("tumor1", new Suggestion("<b>tumor</b> disease", 0, "neoplastic growth"))
                .put("tumor2", new Suggestion("desmoplastic small round cell <b>tumor</b>", 0, "desmoplastic small round cell tumor"))
                .put("tumor3", new Suggestion("Endolymphatic Sac <b>Tumor</b>", 0, "desmoplastic small round cell tumor"))
                .put("tumor4", new Suggestion("Bladder Inflammatory Myofibroblastic <b>Tumor</b>", 0, "desmoplastic small round cell tumor"))
                .put("tumor5", new Suggestion("Ovarian Microcystic Stromal <b>Tumor</b>", 0, "desmoplastic small round cell tumor"))
                .put("tumor6", new Suggestion("Mixed <b>Tumor</b> of the Skin", 0, "desmoplastic small round cell tumor"))
                .put("tumor7", new Suggestion("germ cell <b>tumor</b>", 0, "desmoplastic small round cell tumor"))
                .put("tumor8", new Suggestion("Calcifying Nested Epithelial Stromal <b>Tumor</b> of the Liver", 0, "desmoplastic small round cell tumor"))
                .put("cancer1", new Suggestion("mandibular <b>cancer</b>", 0, "desmoplastic small round cell tumor"))
                .put("cancer2", new Suggestion("<b>cancer</b>", 0, "desmoplastic small round cell tumor"))
                .put("cancer3", new Suggestion("Calcifying Nested Epithelial Stromal <b>Tumor</b> of the Liver", 0, "desmoplastic small round cell tumor"))
                //cell type
                .put("blood vessel", new Suggestion("blood vessel endothelial cell", 0, "HUVEC cell"))
                .put("B cell", new Suggestion("B cell", 0, "B cell"))
                .put("B Cells", new Suggestion("B Cells", 0, "B-Cells"))
                .build();
    }
}
