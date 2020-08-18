package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CellTypeMarkerGene {

    public abstract String geneId();

    public abstract String inferredCellType();

    public abstract String cellTypeWhereMarker();

    public abstract double pValue();

    public abstract String cellType();

    public abstract double medianExpression();

    public abstract double meanExpression();

    public static CellTypeMarkerGene create(String geneId,
                                            String inferredCellType,
                                            String cellTypeWhereMarker,
                                            double pValue,
                                            String cellType,
                                            double medianExpression,
                                            double meanExpression) {
        return new AutoValue_CellTypeMarkerGene(
                geneId,
                inferredCellType,
                cellTypeWhereMarker,
                pValue,
                cellType,
                medianExpression,
                meanExpression);
    }
}

