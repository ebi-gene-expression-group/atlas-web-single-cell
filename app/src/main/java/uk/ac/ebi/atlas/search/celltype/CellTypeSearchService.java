package uk.ac.ebi.atlas.search.celltype;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.GeneSearchService;

@Component
@RequiredArgsConstructor
public class CellTypeSearchService {

    private final CellTypeSearchDao cellTypeSearchDao;
    private final GeneSearchService geneSearchService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CellTypeSearchService.class);

    public ImmutableSet<String> search(ImmutableSet<String> geneIds, ImmutableSet<String> organismParts) {
        if (geneIds.isEmpty()) {
            LOGGER.warn("Can't query for organism part as no gene IDs has given.");
            return ImmutableSet.of();
        }

        LOGGER.info("Searching organism parts for this gene ids: {}", geneIds.asList());

        return cellTypeSearchDao.searchCellTypes(geneSearchService.getCellIdsFromGeneIds(geneIds), organismParts);
    }
}
