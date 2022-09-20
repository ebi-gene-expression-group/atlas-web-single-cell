package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrganismPartSearchService {

    private final OrganismPartSearchDao organismPartSearchDao;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganismPartSearchService.class);

    public Optional<ImmutableSet<String>> search(Optional<ImmutableSet<String>> geneIds) {
        if (geneIds.isPresent() && geneIds.get().isEmpty()) {
            LOGGER.debug("Given set of gene IDs is empty. That results with an empty set of organism part.");
            return Optional.of(ImmutableSet.of());
        }

        LOGGER.info("Searching organism parts for this gene ids: {}", geneIds.orElseThrow().asList());

        return organismPartSearchDao.searchOrganismPart(geneIds.orElseThrow());
    }
}
