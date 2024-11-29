package uk.ac.ebi.atlas.experimentpage.markergenes;

import java.util.Comparator;

class MarkerGeneComparatorByCellGroupValueWhereMarker implements Comparator<MarkerGene> {

    public int compare(MarkerGene marker1, MarkerGene marker2) {
        var clusterNameComparator = new ClusterNameComparator();

        return clusterNameComparator.compare(
            marker1.cellGroupValueWhereMarker(),marker2.cellGroupValueWhereMarker());
    }
}
