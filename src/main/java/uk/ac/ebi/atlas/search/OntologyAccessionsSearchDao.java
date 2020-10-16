package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.CartesianProductStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.InnerJoinStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SortStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_ANCESTORS_URIS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_LOCATED_IN_URIS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_PART_OF_URIS;

@Repository
public class OntologyAccessionsSearchDao {
    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public OntologyAccessionsSearchDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    /*
    curl --data-urlencode 'expr=
    unique(
      sort(
        cartesianProduct(
          innerJoin(
            unique(
              search(
                scxa-analytics-v4,
                qt="/export",
                q="experiment_accession:E-MTAB-5061 AND (ontology_annotation:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_ancestors_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_located_in_rel_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_part_of_rel_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264)",
                fl="cell_id",
                sort="cell_id asc"
              ),
              over=cell_id
            ),
            search(
                scxa-analytics-v4,
                qt="/export",
                q="experiment_accession:E-MTAB-5061 AND ontology_annotation:*",
                fl="cell_id, ontology_annotation",
                sort="cell_id asc"
              ),
            on="cell_id"
          ),
          ontology_annotation
        ),
        by="ontology_annotation asc"
      ),
      over=ontology_annotation
    )
    ' \
    http://localhost:8983/solr/scxa-analytics-v4/stream
    */
    public ImmutableSet<String> searchOntologyAnnotations(String experimentAccession,
                                                          String ontologyUri) {
        var cellIdsFromOntologyUriQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(ImmutableMap.of(
                                ONTOLOGY_ANNOTATION, ImmutableSet.of(ontologyUri),
                                ONTOLOGY_ANNOTATION_ANCESTORS_URIS, ImmutableSet.of(ontologyUri),
                                ONTOLOGY_ANNOTATION_LOCATED_IN_URIS, ImmutableSet.of(ontologyUri),
                                ONTOLOGY_ANNOTATION_PART_OF_URIS, ImmutableSet.of(ontologyUri)))
                        .setFieldList(CELL_ID)
                        .sortBy(CELL_ID, SolrQuery.ORDER.asc);
        var uniqueCellIdsFromOntologyUriStreamBuilder =
                new UniqueStreamBuilder(
                        new SearchStreamBuilder<>(
                                singleCellAnalyticsCollectionProxy,
                                cellIdsFromOntologyUriQueryBuilder)
                                .returnAllDocs(),
                        CELL_ID.name());

        var cellIdsWithOntologyAnnotationQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .setNormalize(false)
                        .addQueryFieldByTerm(ONTOLOGY_ANNOTATION, "*")
                        .setFieldList(ImmutableSet.of(CELL_ID, ONTOLOGY_ANNOTATION))
                        .sortBy(CELL_ID, SolrQuery.ORDER.asc);
        var cellIdsWithOntologyAnnotationStreamBuilder =
                new SearchStreamBuilder<>(
                        singleCellAnalyticsCollectionProxy,
                        cellIdsWithOntologyAnnotationQueryBuilder)
                        .returnAllDocs();

        var cellIdsFromOntologyUriWithOntologyAnnotationsStreamBuilder =
                new InnerJoinStreamBuilder(
                        uniqueCellIdsFromOntologyUriStreamBuilder,
                        cellIdsWithOntologyAnnotationStreamBuilder,
                        CELL_ID.name());

        var ontologyAnnotationsFromOntologyUriStreamBuilder =
                new UniqueStreamBuilder(
                        new SortStreamBuilder(
                                new CartesianProductStreamBuilder(
                                        cellIdsFromOntologyUriWithOntologyAnnotationsStreamBuilder,
                                        ImmutableSet.of(ONTOLOGY_ANNOTATION.name())),
                                ONTOLOGY_ANNOTATION.name()),
                        ONTOLOGY_ANNOTATION.name());

        try (var tupleStreamer =
                     TupleStreamer.of(ontologyAnnotationsFromOntologyUriStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(ONTOLOGY_ANNOTATION.name()))
                    .collect(toImmutableSet());
        }
    }
}
