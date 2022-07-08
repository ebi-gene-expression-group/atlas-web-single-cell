package uk.ac.ebi.atlas.search.geneids;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_PROPERTY_NAMES;

// Takes a GeneQuery object and returns a set of matching gene IDs, if a category is missing it tries several ID fields
@Component
public class GeneIdSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneIdSearchService.class);

    private final static ImmutableSet<String> VALID_QUERY_FIELDS =
            ImmutableSet.<String>builder()
                    .add("q")
                    .addAll(
                            BIOENTITY_PROPERTY_NAMES.stream()
                                    .map(propertyName -> propertyName.name)
                                    .collect(toImmutableSet()))
                    .build();
    private final GeneIdSearchDao geneIdSearchDao;

    private final SpeciesFactory speciesFactory;

    public GeneIdSearchService(GeneIdSearchDao geneIdSearchDao, SpeciesFactory speciesFactory) {
        this.geneIdSearchDao = geneIdSearchDao;
        this.speciesFactory = speciesFactory;
    }

    public Optional<ImmutableSet<String>> search(GeneQuery geneQuery) {
        if (geneQuery.category().isPresent()) {
            if (geneQuery.species().isPresent() &&
                !BioentitiesCollectionProxy.SPECIES_OVERRIDE_PROPERTY_NAMES.contains(geneQuery.category().get())) {
                LOGGER.debug(
                        "Searching {}/{} in species {}",
                        geneQuery.queryTerm(),
                        geneQuery.category().get(),
                        geneQuery.species().get().getEnsemblName());

                return geneIdSearchDao.searchGeneIds(
                        geneQuery.queryTerm(),
                        geneQuery.category().get().name,
                        geneQuery.species().get().getEnsemblName());
            }

            LOGGER.debug(
                    "Searching {}/{} ignoring species {}",
                    geneQuery.queryTerm(),
                    geneQuery.category().get(),
                    geneQuery.species().map(Species::getEnsemblName).orElse("(none provided)"));

            return geneIdSearchDao.searchGeneIds(geneQuery.queryTerm(), geneQuery.category().get().name);
        }

        LOGGER.debug(
                "Searching {} (free text without category) in species {}",
                geneQuery.queryTerm(),
                geneQuery.species().map(Species::getEnsemblName).orElse("(none provided)"));

        return geneQuery.species().isPresent() ?
                searchIds(propertyName ->
                        geneIdSearchDao.searchGeneIds(
                                geneQuery.queryTerm(),
                                propertyName,
                                geneQuery.species().get().getEnsemblName())) :
                searchIds(propertyName -> geneIdSearchDao.searchGeneIds(geneQuery.queryTerm(), propertyName));
    }

    private Optional<ImmutableSet<String>> searchIds(Function<String, Optional<ImmutableSet<String>>> idSearcher) {
        boolean queryMatchesKnownIds = false;

        for (BioentityPropertyName propertyName : BioentitiesCollectionProxy.BIOENTITY_PROPERTY_NAMES) {
            Optional<ImmutableSet<String>> matchingGeneIds = idSearcher.apply(propertyName.name);

            if (matchingGeneIds.isPresent()) {
                if (!matchingGeneIds.get().isEmpty()) {
                    return matchingGeneIds;
                }

                queryMatchesKnownIds = true;
            }
        }

        return queryMatchesKnownIds ? Optional.of(ImmutableSet.of()) : Optional.empty();
    }

    public GeneQuery getGeneQueryByRequestParams(MultiValueMap<String, LinkedList<String>> requestParams) {
        var species = Stream.ofNullable(requestParams.getFirst("species"))
                .map(LinkedList::getFirst)
                .filter(org.apache.commons.lang3.StringUtils::isNotEmpty)
                .map(speciesFactory::create)
                .findFirst();

        // We support currently only one query term; in the unlikely case that somebody fabricates a URL with more than
        // one we’ll build the query with the first match. Remember that in order to support multiple terms we’ll
        // likely need to change GeneQuery and use internally a SemanticQuery
        var category =
                requestParams.keySet().stream()
                        // We rely on "q" and BioentityPropertyName::name’s being lower case
                        .filter(actualField -> VALID_QUERY_FIELDS.contains(actualField.toLowerCase()))
                        .findFirst()
                        .orElseThrow(() -> new QueryParsingException("Error parsing query"));
        var queryTerm = Objects.requireNonNull(requestParams.getFirst(category)).getFirst();

       return category.equals("q") ?
                species
                        .map(_species -> GeneQuery.create(queryTerm, _species))
                        .orElseGet(() -> GeneQuery.create(queryTerm)) :
                species
                        .map(_species -> GeneQuery.create(queryTerm, BioentityPropertyName.getByName(category), _species))
                        .orElseGet(() -> GeneQuery.create(queryTerm, BioentityPropertyName.getByName(category)));
    }
}
