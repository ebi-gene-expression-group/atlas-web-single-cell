package uk.ac.ebi.atlas.experimentpage.markergenes;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

class ClusterNameComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        final String regexForNumeric = "^[0-9]*$";

        if (o1.matches(regexForNumeric) && o2.matches(regexForNumeric)) {
            return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
        } else if (o1.matches(regexForNumeric)) {
            return 1;
        } else if (o2.matches(regexForNumeric)) {
            return -1;
        } else {
            return Collator.getInstance(Locale.US).compare(o1, o2);
        }
    }
}
