package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CellTypeMarkerGene {

    public abstract String geneId();

    public abstract String kWhereMarker();

    public abstract String clusterIdWhereMarker();

    public abstract double pValue();

    public abstract String clusterId();

    public abstract double medianExpression();

    public abstract double meanExpression();

    public static CellTypeMarkerGene create(String geneId,
                                            String kWhereMarker,
                                            String clusterIdWhereMarker,
                                            double pValue,
                                            String clusterId,
                                            double medianExpression,
                                            double meanExpression) {
        return new AutoValue_CellTypeMarkerGene(
                geneId,
                kWhereMarker,
                clusterIdWhereMarker,
                pValue,
                clusterId,
                medianExpression,
                meanExpression);
    }
}

