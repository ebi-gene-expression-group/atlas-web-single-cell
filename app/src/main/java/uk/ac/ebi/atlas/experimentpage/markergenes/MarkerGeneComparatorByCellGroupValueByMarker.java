package uk.ac.ebi.atlas.experimentpage.markergenes;

import java.util.Comparator;

class MarkerGeneComparatorByCellGroupValueByMarker implements Comparator<MarkerGene> {

    public int compare(MarkerGene marker1, MarkerGene marker2) {
        var marker1CellGroupValue = marker1.cellGroupValueWhereMarker();
        var marker2CellGroupValue = marker2.cellGroupValueWhereMarker();

        int len1 = marker1CellGroupValue.length();
        int len2 = marker2CellGroupValue.length();
        int i = 0;
        while (markerValueNotEnded(i, len1, len2)) {
            char c1 = marker1CellGroupValue.charAt(i);
            char c2 = marker2CellGroupValue.charAt(i);
            if (isBothNonNumeric(c1, c2)) {
                // Non-numeric characters, compare as usual
                if (c1 != c2) {
                    return c1 - c2;
                }
            } else if (isNonNumeric(c1)) {
                // marker1CellGroupValue has non-numeric prefix, marker2CellGroupValue is numeric
                return -1;
            } else if (isNonNumeric(c2)) {
                // marker2CellGroupValue has non-numeric prefix, marker1CellGroupValue is numeric
                return 1;
            } else {
                // Both strings have numeric parts, compare as integers
                return compareAsNumeric(marker1CellGroupValue, marker2CellGroupValue, i);
            }
            i++;
        }
        // Handle strings with different lengths
        return Integer.compare(len1, len2);
    }

    private static boolean markerValueNotEnded(int i, int len1, int len2) {
        return i < len1 && i < len2;
    }

    private static boolean isNonNumeric(char c1) {
        return !Character.isDigit(c1);
    }

    private static boolean isBothNonNumeric(char c1, char c2) {
        return isNonNumeric(c1) && isNonNumeric(c2);
    }

    private static int compareAsNumeric(String marker1CellGroupValue, String marker2CellGroupValue, int i) {
        int num1 = Integer.parseInt(marker1CellGroupValue.substring(i));
        int num2 = Integer.parseInt(marker2CellGroupValue.substring(i));
        return Integer.compare(num1, num2);
    }
}
