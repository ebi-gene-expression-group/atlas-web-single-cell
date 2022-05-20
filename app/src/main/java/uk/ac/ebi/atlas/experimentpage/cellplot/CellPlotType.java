package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;

public class CellPlotType {
    private final CellPlotService cellPlotService;

    public CellPlotType(CellPlotService cellPlotService) {
        this.cellPlotService = cellPlotService;
    }

    public final static String DEFAULT_PLOT_METHOD = "umap";

    public static Optional<ImmutableSet<String>> getParameters(String experimentAccession, String plotType) {
        return Optional.ofNullable(CellPlotService.cellPlotParameter(experimentAccession, plotType));
    }
}
