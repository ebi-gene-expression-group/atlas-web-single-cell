package uk.ac.ebi.atlas.search.species;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpeciesSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesSearchService.class);

    private final SpeciesSearchDao speciesSearchDao;


    public Optional<ImmutableSet<String>> search(String searchText, String category) {
        if (StringUtils.isBlank(searchText)) {
            LOGGER.debug("Search text is empty that is going to result with no species.");

            return Optional.of(ImmutableSet.of());
        }

        LOGGER.info("Searching for: {}.", searchText);
        LOGGER.info("The provided search category is: {}", category);

        return speciesSearchDao.searchSpecies(searchText, category);
    }
}
