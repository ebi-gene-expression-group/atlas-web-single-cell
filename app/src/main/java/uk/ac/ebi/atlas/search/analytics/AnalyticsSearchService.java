package uk.ac.ebi.atlas.search.analytics;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.GeneSearchService;

@Component
@RequiredArgsConstructor
public class AnalyticsSearchService {

    private final AnalyticsSearchDao analyticsSearchDao;
    private final GeneSearchService geneSearchService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSearchService.class);

    public ImmutableSet<String> search(ImmutableSet<String> geneIds) {
        if (geneIds.isEmpty()) {
            LOGGER.warn("Can't query for organism part as no gene IDs has given.");
            return ImmutableSet.of();
        }

        LOGGER.info("Searching organism parts for this gene ids: {}", geneIds.asList());

        return analyticsSearchDao.searchOrganismPart(geneSearchService.getCellIdsFromGeneIds(geneIds));
    }

}
