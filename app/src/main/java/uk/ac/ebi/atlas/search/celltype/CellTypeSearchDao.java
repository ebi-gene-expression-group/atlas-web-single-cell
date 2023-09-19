package uk.ac.ebi.atlas.search.celltype;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.CartesianProductStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.InnerJoinStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SelectStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SortStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_FACTOR_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_FACTOR_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_ANCESTORS_URIS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_LOCATED_IN_URIS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION_PART_OF_URIS;

@Repository
public class CellTypeSearchDao {
    private static final String INFERRED_CELL_TYPE_ONTOLOGY_LABELS_VALUE = "inferred_cell_type_-_ontology_labels";
    private static final String INFERRED_CELL_TYPE_AUTHORS_LABELS_VALUE = "inferred_cell_type_-_authors_labels";
    private static final String CELL_TYPE_VALUE_RENAMED_FIELD = "cell_type_value";

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public CellTypeSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy =
                collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Cacheable(cacheNames = "inferredCellTypesOntology", key = "{#experimentAccession, #organOrOrganismPart}")
    public ImmutableSet<String> getInferredCellTypeOntologyLabels(String experimentAccession,
                                                                  ImmutableSet<String> organOrOrganismPart) {
        return getCellTypeMetadata(experimentAccession, organOrOrganismPart, INFERRED_CELL_TYPE_ONTOLOGY_LABELS_VALUE);
    }

    @Cacheable(cacheNames = "inferredCellTypesAuthors", key = "{#experimentAccession, #organOrOrganismPart}")
    public ImmutableSet<String> getInferredCellTypeAuthorsLabels(String experimentAccession,
                                                                 ImmutableSet<String> organOrOrganismPart) {
        return getCellTypeMetadata(experimentAccession, organOrOrganismPart, INFERRED_CELL_TYPE_AUTHORS_LABELS_VALUE);
    }

    /*
     curl --data-urlencode 'expr=
     select(
       unique(
         sort(
           cartesianProduct(
             innerJoin(
                unique(
                 search(
                   scxa-analytics-v3,
                   qt="/export",
                   q="experiment_accession:E-MTAB-5061 AND (ontology_annotation:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_ancestors_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_located_in_rel_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264 OR ontology_annotation_part_of_rel_uris_s:http\://purl.obolibrary.org/obo/UBERON_0001264)",
                   fl="cell_id",
                   sort="cell_id asc"
                 ),
                 over=cell_id
               ),
               select(
                 unique(
                   search(
                     scxa-analytics-v3,
                     qt="/export",
                     q="experiment_accession:E-MTAB-5061 AND (facet_factor_name:inferred_cell_type_-_ontology_labels OR facet_characteristic_name:inferred_cell_type_-_ontology_labels)",
                     fl="cell_id,facet_factor_value,facet_characteristic_value",
                     sort="cell_id asc"
                   ),
                   over=cell_id
                 ),
                 cell_id,
                 facet_factor_value as cell_type_value,
                 facet_characteristic_value as cell_type_value
               ),
               on=cell_id
             ),
             cell_type_value
           ),
           by="cell_type_value asc"
         ),
         over=cell_type_value
       ),
       cell_type_value
     )' \
     http://localhost:8983/solr/scxa-analytics-v3/stream
     */
    private ImmutableSet<String> getCellTypeMetadata(String experimentAccession,
                                                     ImmutableSet<String> organOrOrganismPart,
                                                     String cellTypeFacetName) {
        var cellIdsInOrganOrOrganismPartQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(ImmutableMap.of(
                                ONTOLOGY_ANNOTATION, organOrOrganismPart,
                                ONTOLOGY_ANNOTATION_ANCESTORS_URIS, organOrOrganismPart,
                                ONTOLOGY_ANNOTATION_LOCATED_IN_URIS, organOrOrganismPart,
                                ONTOLOGY_ANNOTATION_PART_OF_URIS, organOrOrganismPart))
                        .setFieldList(CELL_ID)
                        .sortBy(CELL_ID, SolrQuery.ORDER.asc);
        var uniqueCellIdsInOrganOrOrganismPartStreamBuilder =
                new UniqueStreamBuilder(
                        new SearchStreamBuilder<>(
                                singleCellAnalyticsCollectionProxy,
                                cellIdsInOrganOrOrganismPartQueryBuilder)
                                .returnAllDocs(),
                        CELL_ID.name());

        var cellIdsAnnotatedWithCellTypeValueQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(
                                ImmutableMap.of(
                                        FACET_FACTOR_NAME, ImmutableSet.of(cellTypeFacetName),
                                        FACET_CHARACTERISTIC_NAME, ImmutableSet.of(cellTypeFacetName)))
                        .setFieldList(ImmutableSet.of(CELL_ID, FACET_FACTOR_VALUE, FACET_CHARACTERISTIC_VALUE))
                        .sortBy(CELL_ID, SolrQuery.ORDER.asc);
        var uniqueCellIdsAnnotatedWithCellTypeValue =
                new SelectStreamBuilder(
                        new UniqueStreamBuilder(
                                new SearchStreamBuilder<>(
                                        singleCellAnalyticsCollectionProxy,
                                        cellIdsAnnotatedWithCellTypeValueQueryBuilder)
                                        .returnAllDocs(),
                                CELL_ID.name()))
                        .addFieldMapping(ImmutableMap.of(
                                CELL_ID.name(), CELL_ID.name(),
                                FACET_FACTOR_VALUE.name(), CELL_TYPE_VALUE_RENAMED_FIELD,
                                FACET_CHARACTERISTIC_VALUE.name(), CELL_TYPE_VALUE_RENAMED_FIELD));

        var cellTypeValuesInOrganOrOrganismPartStreamBuilder =
                new SelectStreamBuilder(
                        new UniqueStreamBuilder(
                                new SortStreamBuilder(
                                        new CartesianProductStreamBuilder(
                                                new InnerJoinStreamBuilder(
                                                        uniqueCellIdsInOrganOrOrganismPartStreamBuilder,
                                                        uniqueCellIdsAnnotatedWithCellTypeValue,
                                                        CELL_ID.name()),
                                                ImmutableSet.of(CELL_TYPE_VALUE_RENAMED_FIELD)),
                                        CELL_TYPE_VALUE_RENAMED_FIELD),
                                CELL_TYPE_VALUE_RENAMED_FIELD),
                        ImmutableList.of(CELL_TYPE_VALUE_RENAMED_FIELD));

        try (var tupleStreamer =
                     TupleStreamer.of(cellTypeValuesInOrganOrOrganismPartStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(CELL_TYPE_VALUE_RENAMED_FIELD))
                    .collect(toImmutableSet());
        }
    }

    public ImmutableSet<String> searchCellTypes(ImmutableSet<String> cellIds, ImmutableSet<String> organismParts) {
//        Streaming query for getting the cell types provided by set of cell IDs and organism parts
//        unique(
//            search(scxa-analytics-v6, q=cell_id:<SET_OF_CELL_IDS> AND ctw_organism_part:<SET_OF_ORGANISM_PART>,
//            fl="ctw_cell_type",
//            sort="ctw_cell_type asc"
//            ),
//            over="ctw_cell_type"
//        )
        return getCellTypeFromStreamQuery(
                new UniqueStreamBuilder(getStreamBuilderForCellTypeByCellIdsAndOrganismParts(
                        cellIds, organismParts), CTW_CELL_TYPE.name()));
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderForCellTypeByCellIdsAndOrganismParts(
            ImmutableSet<String> cellIDs, ImmutableSet<String> organismParts) {
        var cellTypeQueryBuilder = new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                .addQueryFieldByTerm(CELL_ID, cellIDs)
                .setFieldList(CTW_CELL_TYPE)
                .sortBy(CTW_CELL_TYPE, SolrQuery.ORDER.asc);

        if (organismParts != null && !organismParts.isEmpty()) {
            cellTypeQueryBuilder.addQueryFieldByTerm(CTW_ORGANISM_PART, organismParts);
        }

        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                cellTypeQueryBuilder
        ).returnAllDocs();
    }

    private ImmutableSet<String> getCellTypeFromStreamQuery(UniqueStreamBuilder uniqueCellTypeStreamBuilder) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(uniqueCellTypeStreamBuilder.build())) {
            return tupleStreamer.get()
                    .filter(tuple -> !tuple.getFields().isEmpty())
                    .map(tuple -> tuple.getString(CTW_CELL_TYPE.name()))
                    .collect(toImmutableSet()
                    );
        }
    }
}