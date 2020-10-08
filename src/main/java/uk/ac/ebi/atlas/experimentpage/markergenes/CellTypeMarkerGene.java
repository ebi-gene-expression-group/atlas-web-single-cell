package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CellTypeMarkerGene {

    public abstract String geneId();

    public abstract String cellGroupType();

    public abstract String cellGroupValueWhereMarker();

    public abstract double pValue();

    public abstract String cellGroupValue();

    public abstract double medianExpression();

    public abstract double meanExpression();

    public static CellTypeMarkerGene create(String geneId,
                                            String cellGroupType,
                                            String cellGroupValueWhereMarker,
                                            double pValue,
                                            String cellGroupValue,
                                            double medianExpression,
                                            double meanExpression) {
        return new AutoValue_CellTypeMarkerGene(
                geneId,
                cellGroupType,
                cellGroupValueWhereMarker,
                pValue,
                cellGroupValue,
                medianExpression,
                meanExpression);
    }
}

