package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrJsonFacetBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

import java.util.ArrayList;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_ANCESTORS_LABELS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_LABEL;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_PARENT_LABELS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_PART_OF_LABELS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_SYNONYMS;

@Component
public class CellTypeWheelDao {
    private static final String ORGANISMS_TERMS_KEY = "organisms";
    private static final String ORGANISM_PARTS_TERMS_KEY = "organismParts";
    private static final String CELL_TYPES_TERMS_KEY = "cellTypes";
    private static final String EXPERIMENT_ACCESSIONS_TERMS_KEY = "experimentAccessions";
    private static final String NOT_APPLICABLE_TERM = "not applicable";

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public CellTypeWheelDao(SolrCloudCollectionProxyFactory proxyFactory) {
        singleCellAnalyticsCollectionProxy = proxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

//    curl http://$SOLR_HOST/solr/scxa-analytics-v6/query?rows=0 -d '
//{
//    "query": "ontology_annotation_label_t:lung",
//        "filter": "!ctw_cell_type:\"not applicable\"",
//        "facet": {
//    "organisms": {
//        "type": "terms",
//                "field": "ctw_organism",
//                "limit": -1,
//                "facet": {
//            "organismParts": {
//                "type": "terms",
//                        "field": "ctw_organism_part",
//                        "limit": -1,
//                        "facet": {
//                    "organismParts": {
//                        "type": "terms",
//                                "field": "ctw_organism_part",
//                                "limit": -1,
//                                "facet": {
//                            "cellTypes": {
//                                "type": "terms",
//                                        "field": "ctw_cell_type",
//                                        "limit": -1,
//                                        "facet": {
//                                    "experimentAccessions": {
//                                        "type": "terms",
//                                                "field": "experiment_accession",
//                                                "limit": -1
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//}
//    '

    public ImmutableList<ImmutableList<String>> facetSearchCtwFields(String searchTerm) {
        var facetBuilder =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(CTW_ORGANISM)
                        .addNestedFacet(
                                ORGANISM_PARTS_TERMS_KEY,
                                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                                        .setFacetField(CTW_ORGANISM_PART)
                                        .addNestedFacet(
                                                CELL_TYPES_TERMS_KEY,
                                                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                                                        .setFacetField(CTW_CELL_TYPE)
                                                        .addNestedFacet(
                                                                EXPERIMENT_ACCESSIONS_TERMS_KEY,
                                                                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                                                                        .setFacetField(EXPERIMENT_ACCESSION))));

        // "query": "ontology_annotation_ancestors_labels_t:\"entity\" OR
        // ontology_annotation_parent_labels_t:\"entity\" OR
        // ontology_annotation_part_of_rel_labels_t:\"entity\" OR ontology_annotation_synonyms_t:\"entity\" OR
        // ontology_annotation_label_t:\"entity\""
        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(ImmutableMap.of(
                                ONTOLOGY_ANNOTATION_LABEL, ImmutableSet.of(searchTerm),
                                ONTOLOGY_ANNOTATION_PARENT_LABELS, ImmutableSet.of(searchTerm),
                                ONTOLOGY_ANNOTATION_ANCESTORS_LABELS, ImmutableSet.of(searchTerm),
                                ONTOLOGY_ANNOTATION_PART_OF_LABELS, ImmutableSet.of(searchTerm),
                                ONTOLOGY_ANNOTATION_SYNONYMS, ImmutableSet.of(searchTerm)))
                        .addNegativeFilterFieldByTerm(CTW_CELL_TYPE, ImmutableList.of(NOT_APPLICABLE_TERM))
                        .addFacet(ORGANISMS_TERMS_KEY, facetBuilder)
                        .setRows(0);    // We only want the facets, we don’t care about the docs

        return extractNestedFacetTerms(
                (SimpleOrderedMap<Object>) singleCellAnalyticsCollectionProxy
                        .query(queryBuilder)
                        .getResponse()
                        .findRecursive("facets"),
                ImmutableList.of(ORGANISMS_TERMS_KEY, ORGANISM_PARTS_TERMS_KEY, CELL_TYPES_TERMS_KEY, EXPERIMENT_ACCESSIONS_TERMS_KEY),
                ImmutableList.of());
    }

    // extractNestedFacetTerms is a recursive, general version of:
    // var ctw = ImmutableList.<ImmutableList<String>>builder();
    // ((ArrayList<SimpleOrderedMap>)cellTypeWheelDao.search(searchTerm).findRecursive("organisms", "buckets"))
    //        .forEach((SimpleOrderedMap organismBucket) ->
    //            ((ArrayList<SimpleOrderedMap>) organismBucket.findRecursive("organismParts", "buckets"))
    //                    .forEach((SimpleOrderedMap organismPartBucket) ->
    //                        ((ArrayList<SimpleOrderedMap<Object>>) organismPartBucket.findRecursive("cellTypes", "buckets"))
    //                                .forEach((SimpleOrderedMap<Object> cellTypeBucket) ->
    //                                        ((ArrayList<SimpleOrderedMap<Object>>) cellTypeBucket.findRecursive("experimentAccessions", "buckets"))
    //                                                .forEach((SimpleOrderedMap<Object> experimentAccessionBucket) ->
    //                                                    ctw.add(
    //                                                            ImmutableList.of(
    //                                                                    (String) organismBucket.get("val"),
    //                                                                    (String) organismPartBucket.get("val"),
    //                                                                    (String) cellTypeBucket.get("val"),
    //                                                                    (String) experimentAccessionBucket.get("val")))
    //                                                )
    //                                )
    //                    )
    //        );
    private ImmutableList<ImmutableList<String>> extractNestedFacetTerms(SimpleOrderedMap<Object> facet,
                                                                         ImmutableList<String> facetNames,
                                                                         ImmutableList<String> collectedTerms) {
        // Base case: if there are no more facet names to process return the collected terms; they need to be wrapped
        // in a singleton list to match the return type of the recursive function
        if (facetNames.isEmpty()) {
            return ImmutableList.of(collectedTerms);
        }
        // General case: take the first element off facetNames, and collect the corresponding term that matches it;
        // do this for each bucket, flattening the returned list
        else if (facet.findRecursive(facetNames.get(0), "buckets") != null) {
            var nextFacetNames = facetNames.subList(1, facetNames.size());

            return ((ArrayList<SimpleOrderedMap<Object>>) facet.findRecursive(facetNames.get(0), "buckets"))
                    .stream()
                    .flatMap(
                            (SimpleOrderedMap<Object> bucket) ->
                                    // Down the rabbit hole we go!
                                    // https://media.giphy.com/media/3o6fJgEOrF1lky8WFa/giphy.gif?cid=ecf05e47s8zjsd73rpmnw1ebzekngdu4ek585tj66r51htl8
                                    extractNestedFacetTerms(
                                            bucket,
                                            nextFacetNames,
                                            ImmutableList.<String>builder()
                                                    .addAll(collectedTerms)
                                                    .add((String) bucket.get("val"))
                                                    .build())
                                            .stream())
                    .collect(toImmutableList());
        }
        // Empty if no buckets are found
        else {
            return ImmutableList.of();
        }
    }
}
