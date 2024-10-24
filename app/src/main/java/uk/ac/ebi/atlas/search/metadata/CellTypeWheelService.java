package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.FeaturedSpeciesService;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Service
public class CellTypeWheelService {
    private final CellTypeWheelDao cellTypeWheelDao;
    private final FeaturedSpeciesService featuredSpeciesService;

    public CellTypeWheelService(CellTypeWheelDao cellTypeWheelDao,
                                FeaturedSpeciesService featuredSpeciesService) {
        this.cellTypeWheelDao = cellTypeWheelDao;
        this.featuredSpeciesService = featuredSpeciesService;
    }

    public ImmutableSet<ImmutablePair<ImmutableList<String>, String>> search(String searchTerm, String species) {
        ImmutableList<String> allSpeciesNames = featuredSpeciesService.getSpeciesNamesSortedByExperimentCount();
        var isSpeciesSearch = allSpeciesNames.contains(StringUtils.capitalize(searchTerm));

        Stream<ImmutableList<String>> cellTypeWheelResultsStream = isSpeciesSearch
                ? cellTypeWheelDao.speciesSearchCtwFields(searchTerm, species).stream()
                : cellTypeWheelDao.facetSearchCtwFields(searchTerm, species).stream();

        return cellTypeWheelResultsStream
                // This will effectively “explode” tuples and aggregate experiment accessions
                // (the last element in the tuple) to the organisms, organism parts, and cell types
                .map(this::addTailToEveryHeadSublist)
                .flatMap(ImmutableList::stream)
                .collect(toImmutableSet());
    }

    // Transform a list [e_1, e_2, e_3, ..., e_n] into:
    // Pair.of([e_1], e_n)
    // Pair.of([e_1, e_2], e_n)
    // Pair.of([e_1, e_2, e_3], e_n)
    // ...
    // Pair.of([e_1, e_2, e_3, ..., e_(n-1)], e_n)
    private ImmutableList<ImmutablePair<ImmutableList<String>, String>> addTailToEveryHeadSublist(ImmutableList<String> tuple) {
        return IntStream
                .range(1, tuple.size())
                .boxed()
                .map(index -> ImmutablePair.of(tuple.subList(0, index), tuple.get(tuple.size() - 1)))
                // This removes the last element ([e_1, ... , e_n], e_n), it can be done in many ways
                .filter(headSubListAndTails -> headSubListAndTails.getLeft().size() < tuple.size())
                .collect(toImmutableList());
    }
}
