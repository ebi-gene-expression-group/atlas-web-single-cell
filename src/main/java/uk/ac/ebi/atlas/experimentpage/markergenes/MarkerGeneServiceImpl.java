package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import java.util.Map;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
public class MarkerGeneServiceImpl implements MarkerGeneService {
    private MarkerGenesDao markerGenesDao;

    public MarkerGeneServiceImpl(MarkerGenesDao markerGenesDao) {
        this.markerGenesDao = markerGenesDao;
    }

    /**
     * Get ImmutableList of marker genes objects for an experiment of that organism part.
     * @param organismPart
     * @param experimentAccession
     * @return ImmutableList of CellTypeMarkerGene Objects
     */
    @Override
    public Map<String, ImmutableSet<CellTypeMarkerGene>> getCellTypeMarkerGeneProfile(String experimentAccession, String organismPart) {
        return markerGenesDao.getCellTypeMarkerGenes(experimentAccession, organismPart)
                .stream()
                .collect(groupingBy(CellTypeMarkerGene::cellGroupValue, toImmutableSet())).entrySet().stream()
                .collect(toMap(cellTypeMarkerGenes -> cellTypeMarkerGenes.getKey(),
                        cellTypeMarkerGenes -> cellTypeMarkerGenes.getValue()
                                .stream()
                                .limit(5)
                                .collect(toImmutableSet())));
    }
}
