package uk.ac.ebi.atlas.search.analytics;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.GeneSearchService;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;

import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@Component
@RequiredArgsConstructor
public class AnalyticsSearchService {

    private final AnalyticsSearchDao analyticsSearchDao;
    private final GeneSearchService geneSearchService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSearchService.class);

    public ImmutableSet<String> searchOrganismPart(ImmutableSet<String> geneIds) {
        return searchForFieldByGeneIds(CTW_ORGANISM_PART, geneIds);
    }

    public ImmutableSet<String> searchCellType(ImmutableSet<String> geneIds) {
        return searchForFieldByGeneIds(CTW_CELL_TYPE, geneIds);
    }

    private ImmutableSet<String> searchForFieldByGeneIds(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField schemaField,
            ImmutableSet<String> geneIds) {
        if (geneIds.isEmpty()) {
            LOGGER.warn("Can't query for {} as no gene IDs has given.", schemaField.name());
            return ImmutableSet.of();
        }

        LOGGER.info("Searching {} for these gene ids: {}", schemaField.name(), geneIds.asList());

        return analyticsSearchDao.searchFieldByCellIds(schemaField, geneSearchService.getCellIdsFromGeneIds(geneIds));
    }

}
