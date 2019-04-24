package uk.ac.ebi.atlas.metadata;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CellMetadataService {
    private CellMetadataDao cellMetadataDao;

    public CellMetadataService(CellMetadataDao cellMetadataDao) {
        this.cellMetadataDao = cellMetadataDao;
    }

    // Retrieves all metadata values of interest (factors and characteristics) for a given cell ID
    public Map<String, String> getMetadataValues(String experimentAccession, String cellId) {
        Set<String> factorTypes = cellMetadataDao.getFactorTypes(experimentAccession, Optional.empty());
        Set<String> characteristicTypes = cellMetadataDao.getCharacteristicTypes(experimentAccession);

        return cellMetadataDao.getMetadataValuesForCellId(
                experimentAccession,
                cellId,
                factorTypes,
                characteristicTypes);
    }

    /*
     * Retrieves a list of metadata types of interest for the experiment. This includes all factors (excluding the
     * single cell identifier), curated attributes found in the IDF file, as well as the inferred cell type
     * characteristic.
     */
    public Set<String> getMetadataTypes(String experimentAccession) {
        return Stream.concat(
                cellMetadataDao.getFactorTypes(experimentAccession, Optional.empty()).stream(),
                cellMetadataDao.getCharacteristicTypes(experimentAccession).stream())
                .collect(Collectors.toSet());
    }
}
