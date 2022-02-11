package uk.ac.ebi.atlas.cli.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import java.util.Collection;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

public class SolrInputDocumentMapper {
    protected SolrInputDocumentMapper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Transform a simple (see parameter description below) <code>SolrInputDocument</code> object to an
     * <code>ImmutableMap</code> whose keys are the doc’s field names and the values are the doc’s values. Notice that
     * all values are wrapped in an <code>ImmutableList</code>. The returned map is suitable to be serialised to
     * Solr-compatible JSON as-is: Solr will use the unique element in the array if the schema specifies   a
     * non-multivalued field.
     *
     * Fields with null values are omitted.
     *
     * @param doc   a Solr document without nested child documents
     * @return      an unmodifiable map representation of the Solr document without null or empty values
     */
    public static ImmutableMap<String, Object> transformToMap(SolrInputDocument doc) {
        return doc.values().stream()
                // We need to defend against null and empty values (e.g. expression_levels in proteomics experiments)
                .filter(solrInputField -> solrInputField.getValueCount() > 0)
                .collect(toImmutableMap(
                        SolrInputField::getName,
                        solrInputField -> {
                            if (solrInputField.getValueCount() > 1) {
                                return ImmutableList.copyOf(solrInputField.getValues());
                            }
                            return solrInputField.getValue();
                        }));
    }
}
