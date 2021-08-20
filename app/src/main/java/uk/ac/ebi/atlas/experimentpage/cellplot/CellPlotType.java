package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;

public class CellPlotType {
    private final static ImmutableMap<String, ImmutableSet<String>> PLOT_OPTIONS =
            ImmutableMap.of(
                    "tsne", ImmutableSet.of("perplexity"),
                    "umap", ImmutableSet.of("n_neighbors"));

    public final static String DEFAULT_PLOT_METHOD = "umap";

    public static Optional<ImmutableSet<String>> getParameters(String plotType) {
        return Optional.ofNullable(PLOT_OPTIONS.get(plotType));
    }
}
