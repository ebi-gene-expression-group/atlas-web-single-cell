package uk.ac.ebi.atlas.cli.utils;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import java.util.logging.Logger;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER_DV;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES;

@Service
public class SpeciesBioentityFinder {
    private static final Logger LOGGER = Logger.getLogger(SpeciesBioentityFinder.class.getName());

    private final BioentitiesCollectionProxy bioentitiesCollectionProxy;

    public SpeciesBioentityFinder(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        this.bioentitiesCollectionProxy = collectionProxyFactory.create(BioentitiesCollectionProxy.class);
    }

    /**
     * Retrieve all the bioentity_identifier values (gene IDs) from the bioentities collection.
     *
     * @param   species Species to match the species field to. Case insensitive. Replace blanks with underscores
     *                  (e.g. "Homo sapiens" as "Homo_sapiens").
     * @return          A set with all the gene IDs from a single species
     */
    public ImmutableSet<String> findBioentityIdentifiers(String species) {
        var solrQueryBuilder =
                new SolrQueryBuilder<BioentitiesCollectionProxy>()
                        .addQueryFieldByTerm(SPECIES, species)
                        .setFieldList(ImmutableSet.of(BIOENTITY_IDENTIFIER_DV))
                        .sortBy(BIOENTITY_IDENTIFIER_DV, asc);

        var searchStreamBuilder =
                new SearchStreamBuilder<>(bioentitiesCollectionProxy, solrQueryBuilder)
                        .returnAllDocs();
        var uniqueStreamBuilder = new UniqueStreamBuilder(searchStreamBuilder, BIOENTITY_IDENTIFIER_DV.name());

        LOGGER.info("Retrieving all bioentity identifiers of species " + species + "...");
        try (var tupleStreamer = TupleStreamer.of(uniqueStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(BIOENTITY_IDENTIFIER_DV.name()))
                    .collect(toImmutableSet());
        }
    }
}
