package uk.ac.ebi.atlas.search.analytics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@Component
public class AnalyticsSearchDao {

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public AnalyticsSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        singleCellAnalyticsCollectionProxy =
                collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> searchFieldByCellIds(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField schemaField,
            ImmutableSet<String> cellIDs) {
//        Streaming query for getting the organism_part provided by set of cell IDs
//        unique(
//            search(scxa-analytics-v6, q=cell_id:<SET_OF_CELL_IDS>,
//            fl="schemaField", // could be : ctw_organism_part, ctw_cell_type
//            sort="schemaField asc"
//            ),
//            over="schemaField"
//        )
        return getSchemaFieldFromStreamQuery(
                new UniqueStreamBuilder(
                        getStreamBuilderByCellIdsForSchemaField(cellIDs, schemaField),
                        schemaField.name()
                ),
                schemaField.name()
        );
    }

    public ImmutableSet<String> searchOrganismPartsByCellIdsAndSpecies(ImmutableSet<String> cellIDs,
                                                                       ImmutableSet<String> species) {
        var inputParams = ImmutableMap.of(
                CELL_ID, cellIDs,
                CTW_ORGANISM, species
        );

        return searchOutputFieldByInputFieldValues(CTW_ORGANISM_PART, inputParams);
    }

    public ImmutableSet<String> searchCellTypesByCellIdsAndSpeciesAndOrganismParts(ImmutableSet<String> cellIDs,
                                                                                   ImmutableSet<String> species,
                                                                                   ImmutableSet<String> organismParts) {
        var inputParams = ImmutableMap.of(
                CELL_ID, cellIDs,
                CTW_ORGANISM, species,
                CTW_ORGANISM_PART, organismParts
        );

        return searchOutputFieldByInputFieldValues(CTW_CELL_TYPE, inputParams);
    }

    private ImmutableSet<String> searchOutputFieldByInputFieldValues(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField outputSchemaField,
            ImmutableMap<SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField, ImmutableSet<String>> inputParams) {
        var queryBuilder = getStreamBuilderForOutputField(outputSchemaField);
        inputParams.forEach((key, value) -> {
            if (!value.isEmpty()) {
                queryBuilder.addQueryFieldByTerm(key, value);
            }
        });

        var uniqueSearchStreamBuilder = new UniqueStreamBuilder(
                new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, queryBuilder).returnAllDocs(),
                outputSchemaField.name());

        return getSchemaFieldFromStreamQuery(uniqueSearchStreamBuilder, outputSchemaField.name());
    }

    private SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderForOutputField(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField outputSchemaField) {
        return new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFieldList(outputSchemaField)
                        .sortBy(outputSchemaField, SolrQuery.ORDER.asc);
    }

    public ImmutableSet<String> searchOutputFieldByInputFieldValues(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField outputSchemaField,
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField inputSchemaField,
            ImmutableSet<String> inputValues) {
//        Streaming query for getting the set of output field values provided by set of input field values
//        unique(
//            search(scxa-analytics, q=<name_of_input_field>:<SET_OF_INPUT_FIELD_VALUES>, // could be ctw_cell_type
//            fl="outputSchemaField", // could be : cell_id
//            sort="outputSchemaField asc"
//            ),
//            over="outputSchemaField"
//        )
        return getSchemaFieldFromStreamQuery(
                new UniqueStreamBuilder(
                        getStreamBuilderByInputFieldValuesForOutputField(
                                inputSchemaField, inputValues, outputSchemaField),
                        outputSchemaField.name()
                ),
                outputSchemaField.name()
        );
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderByInputFieldValuesForOutputField(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField inputSchemaField,
            ImmutableSet<String> inputValues,
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField outputSchemaField) {
        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(inputSchemaField, inputValues)
                        .setFieldList(outputSchemaField)
                        .sortBy(outputSchemaField, SolrQuery.ORDER.asc)
        ).returnAllDocs();
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderByCellIdsForSchemaField(
            ImmutableSet<String> cellIDs, SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField schemaField) {
        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CELL_ID, cellIDs)
                        .setFieldList(schemaField)
                        .sortBy(schemaField, SolrQuery.ORDER.asc)
        ).returnAllDocs();
    }

    private ImmutableSet<String> getSchemaFieldFromStreamQuery(UniqueStreamBuilder uniqueOrganismPartStreamBuilder,
            String schemaField) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(uniqueOrganismPartStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(schemaField))
                    .collect(toImmutableSet()
            );
        }
    }
}
