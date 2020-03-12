package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_ANCESTORS_LABELS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_ANCESTORS_URIS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_LABEL;

@Component
public class ScExperimentDao {
    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public ScExperimentDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy = solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> fetchExperimentAccessions(String characteristicName, String characteristicValue) {
        var queryBuilder = new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>();
        /* Reason for checking blank here is in case characteristicValue is empty our solrQueryBuilder
         * builds query like characteristic_value:("\*") which is different from characteristic_value:*
         * which we need. So to handle that situation if characteristicValue is empty we build query
         * characteristic_name:("organism_part") as we need all the experiments in case of empty characteristicValue
         * */
        if(isNotBlank(characteristicValue)) {
            queryBuilder
                    .addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicName)
                    .setNormalize(false)
                    .addQueryFieldByTerm(ImmutableMap.of(
                            CHARACTERISTIC_VALUE, ImmutableList.of("*".concat(characteristicValue)),
                            ONTOLOGY_ANNOTATION, ImmutableList.of("*".concat(characteristicValue)),
                            ONTOLOGY_ANNOTATION_ANCESTORS_LABELS, ImmutableList.of("*".concat(characteristicValue)),
                            ONTOLOGY_ANNOTATION_ANCESTORS_URIS, ImmutableList.of("*".concat(characteristicValue)),
                            ONTOLOGY_ANNOTATION_LABEL, ImmutableList.of("*".concat(characteristicValue))
                    ))
                    .setFieldList(EXPERIMENT_ACCESSION);
        } else {
            queryBuilder
                    .addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicName)
                    .setFieldList(EXPERIMENT_ACCESSION);
        }

        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();
        return results
                .stream()
                .map(solrDocument -> (String) solrDocument.getFieldValue(EXPERIMENT_ACCESSION.name()))
                .collect(toImmutableSet());
    }
}
