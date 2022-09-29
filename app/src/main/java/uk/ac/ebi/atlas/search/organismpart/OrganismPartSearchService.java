package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.GeneSearchService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrganismPartSearchService {

    private final OrganismPartSearchDao organismPartSearchDao;
    private final GeneSearchService geneSearchService;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganismPartSearchService.class);

    public Optional<ImmutableSet<String>> search(Optional<ImmutableSet<String>> geneIds) {
        if (geneIds.isEmpty() || geneIds.get().isEmpty()) {
            LOGGER.debug("Can't query for organism part as no gene IDs has given.");
            return Optional.of(ImmutableSet.of());
        }

        LOGGER.info("Searching organism parts for this gene ids: {}", geneIds.get().asList());

        var cellIDs = geneSearchService.getCellIdsFromGeneIds(geneIds.get());

        if (cellIDs.isEmpty()) {
            return Optional.of(ImmutableSet.of());
        }

        return organismPartSearchDao.searchOrganismPart(cellIDs);
    }

}
