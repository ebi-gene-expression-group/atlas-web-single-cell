package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.apache.commons.lang3.StringUtils.countMatches;

@Component
public class HighchartsSunburstAdapter {
    public HighchartsSunburstAdapter() {
    }

    public ImmutableSet<ImmutableMap<String, ?>> getCellTypeWheelSunburst(
            String searchTerm,
            ImmutableCollection<ImmutablePair<ImmutableList<String>, String>> entries) {
        // The centre is special, so we build it manually
        var centre = ImmutableMap.of(
                "name", searchTerm,
                "id", searchTerm,
                "parent", "",
                "value", 1,
                "experimentAccessions", entries.stream().map(ImmutablePair::getRight).collect(toImmutableSet())
        );

        var rings = entries.stream()
                .map(sourceAndExperimentAccession ->
                        ImmutablePair.of(
                                headStringAndTail(sourceAndExperimentAccession.getLeft()),
                                sourceAndExperimentAccession.getRight()))
                .collect(
                        groupingBy(
                                ImmutablePair::getLeft,
                                mapping(ImmutablePair::getRight, toImmutableSet())))
                .entrySet()
                .stream()
                .map(
                        entry -> ImmutableMap.of(
                                "name", entry.getKey().getRight(),
                                "id", entry.getKey().getLeft().isEmpty() ?
                                        entry.getKey().getRight() + "#" + UUID.randomUUID() :
                                        entry.getKey().getLeft() + "#" + entry.getKey().getRight(),
                                "parent", entry.getKey().getLeft().isEmpty() ?
                                        searchTerm :
                                        entry.getKey().getLeft(),
                                "value", 1,
                                "experimentAccessions", entry.getValue()))
                // Sunburst plot is sensitive to order, the parents (inner slices) must be in the JSON before the
                // children (outer slices)
                .sorted((o1, o2) -> {
                    // I just hope we don’t use hashes in either organism, organism parts or cell types!
                    var hashesInO1 = countMatches((String) o1.get("parent"), "#");
                    var hashesInO2 = countMatches((String) o1.get("parent"), "#");
                    if (hashesInO1 == hashesInO2) {
                        return String.CASE_INSENSITIVE_ORDER.compare((String) o1.get("id"), (String) o2.get("id"));
                    }
                    return Integer.compare(hashesInO1, hashesInO2);
                })
                .collect(toImmutableSet());

        return ImmutableSet.<ImmutableMap<String, ?>>builder()
                .add(centre)
                .addAll(rings)
                .build();
    }

    // Transform a list [e_1, e_2, e_3, ..., e_n] to a pair: "e_1#e_2#e_3#...#e_(n-1)", "e_n"
    // The first element will be the parent in the sunburst plot; we can’t use the name because the same cell type can
    // appear in more than one organism or organism part
    private ImmutablePair<String, String> headStringAndTail(ImmutableList<String> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Tuple cannot be empty");
        }

        return ImmutablePair.of(
                String.join("#", list.subList(0, list.size() - 1)),
                list.get(list.size() - 1));
    }

}
