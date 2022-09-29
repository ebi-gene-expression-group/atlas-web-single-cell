package uk.ac.ebi.atlas.search.species;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
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

    public ImmutableSet<String> searchSpecies(String searchTerm) {
        return searchSpecies(searchTerm, null);
    }

    public ImmutableSet<String> searchSpecies(String searchTerm, String category) {
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

        if (StringUtils.isBlank(searchTerm)) {
            LOGGER.info("We can't conduct a search with an empty search term.");
            return ImmutableSet.of();
        }

        var streamBuilderForSpecies = new InnerJoinStreamBuilder(
                getStreamBuilderForUniqueSpeciesFromBioEntities(searchTerm, category),
                getStreamBuilderForUniqueBioEntityIDFromGene2Experiment(),
                BIOENTITY_IDENTIFIER.name()
        );

        LOGGER.info("Searching species by the given search term: {} and category: {}", searchTerm, category);
        return getSpeciesFromStreamBuilder(streamBuilderForSpecies);
    }

    private ImmutableSet<String> getSpeciesFromStreamBuilder(InnerJoinStreamBuilder speciesStreamBuilder) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(speciesStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(SPECIES.name()))
                    .collect(toImmutableSet()
            );
        }
    }

    private UniqueStreamBuilder getStreamBuilderForUniqueBioEntityIDFromGene2Experiment() {
        var bioEntitiesFromGene2Experiment =
                new SolrQueryBuilder<Gene2ExperimentCollectionProxy>()
                        .setFieldList(Gene2ExperimentCollectionProxy.BIOENTITY_IDENTIFIER)
                        .sortBy(Gene2ExperimentCollectionProxy.BIOENTITY_IDENTIFIER, SolrQuery.ORDER.asc);
        var searchBuilderForBioEntities =
                new SearchStreamBuilder<>(
                        gene2ExperimentCollectionProxy, bioEntitiesFromGene2Experiment).returnAllDocs();
        return new UniqueStreamBuilder(searchBuilderForBioEntities, BIOENTITY_IDENTIFIER.name());
    }

    private UniqueStreamBuilder getStreamBuilderForUniqueSpeciesFromBioEntities(String searchText, String category) {
        var bioEntitiesByTextAndCategory = new SolrQueryBuilder<BioentitiesCollectionProxy>()
                .addQueryFieldByTerm(PROPERTY_VALUE, searchText)
                .setFieldList(ImmutableSet.of(SPECIES_DV, BIOENTITY_IDENTIFIER_DV))
                .sortBy(BIOENTITY_IDENTIFIER_DV, SolrQuery.ORDER.asc);

        if (category != null) {
            bioEntitiesByTextAndCategory.addQueryFieldByTerm(PROPERTY_NAME, category);
        }

        var searchBuilderForSpecies =
                new SearchStreamBuilder<>(bioentitiesCollectionProxy, bioEntitiesByTextAndCategory).returnAllDocs();
        var selectBuilderForBioEntitiesIdAndSpecies =
                new SelectStreamBuilder(searchBuilderForSpecies)
                        .addFieldMapping(ImmutableMap.of(
                                BIOENTITY_IDENTIFIER_DV.name(), BIOENTITY_IDENTIFIER.name(),
                                SPECIES_DV.name(), SPECIES.name()
                        ));
        return new UniqueStreamBuilder(selectBuilderForBioEntitiesIdAndSpecies, BIOENTITY_IDENTIFIER.name());
    }
}
