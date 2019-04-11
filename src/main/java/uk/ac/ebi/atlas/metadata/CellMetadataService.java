package uk.ac.ebi.atlas.metadata;

import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_INFERRED_CELL_TYPE;

@Component
public class CellMetadataService {
    private CellMetadataDao cellMetadataDao;

    public CellMetadataService(CellMetadataDao cellMetadataDao) {
        this.cellMetadataDao = cellMetadataDao;
    }

    public Optional<String> getInferredCellType(String experimentAccession, String cellId) {
        return cellMetadataDao.getMetadataValueForCellId(
                experimentAccession, CHARACTERISTIC_INFERRED_CELL_TYPE,
                cellId);
    }

    public Map<String, String> getFactors(String experimentAccession, String cellId) {
        List<SingleCellAnalyticsSchemaField> factorFieldNames =
                cellMetadataDao.getFactorFieldNames(experimentAccession, cellId);

        return cellMetadataDao.getQueryResultForMultiValueFields(
                experimentAccession, Optional.of(cellId), factorFieldNames)
                .entrySet().stream()
                .collect(toMap(
                        entry -> SingleCellAnalyticsCollectionProxy.metadataFieldNameToDisplayName(entry.getKey()),
                        entry -> entry.getValue().stream().map(Object::toString).collect(Collectors.joining(","))));
    }

    public Map<String, String> getMetadata(String experimentAccession, String cellId) {
        Set<SingleCellAnalyticsSchemaField> metadataTypes = getMetadataTypes(experimentAccession);

        return cellMetadataDao
                .getQueryResultForMultiValueFields(experimentAccession, Optional.of(cellId), metadataTypes)
                .entrySet().stream()
                .collect(toMap(
                        entry -> SingleCellAnalyticsCollectionProxy.metadataFieldNameToDisplayName(entry.getKey()),
                        entry -> entry.getValue().stream().map(Object::toString).collect(Collectors.joining(","))));
    }

    /*
     * Retrieves a list of metadata types of interest for the experiment. This includes all factors (excluding the
     * single cell identifier), curated attributes found in the IDF file, as well as the inferred cell type
     * characteristic.
     */
    public Set<SingleCellAnalyticsSchemaField> getMetadataTypes(String experimentAccession) {
        Set<SingleCellAnalyticsSchemaField> metadataTypes = Stream.concat(
                cellMetadataDao.getFactorTypes(experimentAccession, Optional.empty()).stream(),
                cellMetadataDao.getAdditionalAttributesFieldNames(experimentAccession).stream())
                .collect(Collectors.toSet());

        // Inferred cell type is special and we are always interested in retrieving it, if it is available
        metadataTypes.add(CHARACTERISTIC_INFERRED_CELL_TYPE);

        return metadataTypes;
    }
}
