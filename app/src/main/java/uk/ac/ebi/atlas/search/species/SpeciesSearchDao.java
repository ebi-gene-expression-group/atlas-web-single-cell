package uk.ac.ebi.atlas.search.species;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.Gene2ExperimentCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.InnerJoinStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SelectStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;
import uk.ac.ebi.atlas.trader.ExperimentTraderDao;

import java.util.Optional;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER_DV;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES_DV;

@Component
public class SpeciesSearchDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesSearchDao.class);

    private final BioentitiesCollectionProxy bioentitiesCollectionProxy;
    private final Gene2ExperimentCollectionProxy gene2ExperimentCollectionProxy;

    public SpeciesSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        bioentitiesCollectionProxy = collectionProxyFactory.create(BioentitiesCollectionProxy.class);
        gene2ExperimentCollectionProxy = collectionProxyFactory.create(Gene2ExperimentCollectionProxy.class);
    }

    public Optional<ImmutableSet<String>> searchSpecies(String searchText, String category) {
        // innerJoin(
        //      unique(
        //          select(
        //              search(bioentities-v1, q=property_value:ASPM AND property_name:symbol,
        //                              fl="species_dv,bioentity_identifier_dv",
        //                              sort="bioentity_identifier_dv asc",
        //                              qt="/export"
        //              ),
        //              bioentity_identifier_dv as bioentity_identifier, species_dv as species
        //          ),
        //          over="bioentity_identifier"
        //      ),
        //      unique(
        //          search(scxa-gene2experiment-v1, q=*:*,
        //                                          fl="bioentity_identifier",
        //                                          sort="bioentity_identifier asc",
        //                                          qt="/export"
        //          ),
        //          over="bioentity_identifier"
        //      ),
        //  on="bioentity_identifier"
        // )

        var bioEntitiesByTextAndCategory = new SolrQueryBuilder<BioentitiesCollectionProxy>()
                .addQueryFieldByTerm(PROPERTY_VALUE, searchText)
                .addQueryFieldByTerm(PROPERTY_NAME, category)
                .setFieldList(ImmutableSet.of(SPECIES_DV, BIOENTITY_IDENTIFIER_DV))
                .sortBy(BIOENTITY_IDENTIFIER_DV, SolrQuery.ORDER.asc);
        var searchBioEntitiesBuilder =
                new SearchStreamBuilder<>(bioentitiesCollectionProxy, bioEntitiesByTextAndCategory).returnAllDocs();
        var selectBioEntitiesStreamBuilder =
                new SelectStreamBuilder(searchBioEntitiesBuilder)
                        .addFieldMapping(ImmutableMap.of(
                                BIOENTITY_IDENTIFIER_DV.name(), BIOENTITY_IDENTIFIER.name(),
                                SPECIES_DV.name(), SPECIES.name()
                        ));
        var uniqueBioEntitiesStreamBuilder =
            new UniqueStreamBuilder(selectBioEntitiesStreamBuilder, BIOENTITY_IDENTIFIER.name());

        var gene2ExperimentQueryBuilder = new SolrQueryBuilder<Gene2ExperimentCollectionProxy>()
                .setFieldList(Gene2ExperimentCollectionProxy.BIOENTITY_IDENTIFIER)
                .sortBy(Gene2ExperimentCollectionProxy.BIOENTITY_IDENTIFIER, SolrQuery.ORDER.asc);
        var searchGene2ExperimentBuilder =
                new SearchStreamBuilder<>(gene2ExperimentCollectionProxy, gene2ExperimentQueryBuilder).returnAllDocs();
        var uniqueGene2ExperimentStreamBuilder =
                new UniqueStreamBuilder(searchGene2ExperimentBuilder, BIOENTITY_IDENTIFIER.name());

        var speciesStreamQueryBuilder = new InnerJoinStreamBuilder(
                uniqueBioEntitiesStreamBuilder,
                uniqueGene2ExperimentStreamBuilder,
                BIOENTITY_IDENTIFIER.name()
        );

        try (TupleStreamer tupleStreamer = TupleStreamer.of(speciesStreamQueryBuilder.build())) {
            return Optional.of(tupleStreamer.get()
                    .map(tuple -> tuple.getString(SPECIES.name()))
                    .collect(toImmutableSet())
            );
        }
    }
}
