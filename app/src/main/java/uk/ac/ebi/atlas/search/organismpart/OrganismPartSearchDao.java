package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrganismPartSearchDao {
    public Optional<ImmutableSet<String>> searchOrganismPart(String queryTerm, String category) {

        return Optional.of(ImmutableSet.of());
    }
}
