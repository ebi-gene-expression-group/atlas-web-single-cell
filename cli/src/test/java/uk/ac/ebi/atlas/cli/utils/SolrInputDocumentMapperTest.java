package uk.ac.ebi.atlas.cli.utils;

import com.google.common.collect.ImmutableList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SolrInputDocumentMapperTest {
    @Test
    @DisplayName("SolrInputDocumentMapper is a utility class that cannot be instantiated")
    void utilityClassCannotBeInstantiated() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> new SolrInputDocumentMapper());
    }

    @Test
    @DisplayName("Null values are filtered out")
    void nullValuesAreFilteredOut() {
        var solrInputDocument = new SolrInputDocument();
        var key = randomAlphanumeric(10);
        solrInputDocument.addField(randomAlphanumeric(10), null);

        assertThat(SolrInputDocumentMapper.transformToMap(solrInputDocument))
                .isEmpty();
    }

    @Test
    @DisplayName("Singleton values are wrapped in collections")
    void valuesAreWrappedInCollections() {
        var solrInputDocument = new SolrInputDocument();
        var fieldName = randomAlphanumeric(10);
        var value = randomAlphanumeric(50);

        solrInputDocument.addField(fieldName, value);

        assertThat(SolrInputDocumentMapper.transformToMap(solrInputDocument))
                .containsEntry(fieldName, ImmutableList.of(value))
                .hasSize(1);
    }
}
