package uk.ac.ebi.atlas.bioentity.properties;

import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.Gene2ExperimentCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

@Service
public class ExpressedBioentityFinderImpl implements ExpressedBioentityFinder {
    private final Gene2ExperimentCollectionProxy gene2ExperimentCollectionProxy;

    public ExpressedBioentityFinderImpl(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        this.gene2ExperimentCollectionProxy = collectionProxyFactory.create(Gene2ExperimentCollectionProxy.class);
    }

    @Override
    public boolean bioentityIsExpressedInAtLeastOneExperiment(String bioentityIdentifier) {
        var solrQueryBuilder =
                new SolrQueryBuilder<Gene2ExperimentCollectionProxy>()
                        .addQueryFieldByTerm(Gene2ExperimentCollectionProxy.BIOENTITY_IDENTIFIER, bioentityIdentifier);

        return gene2ExperimentCollectionProxy.query(solrQueryBuilder).getResults().size() > 0;
    }
}
