package uk.ac.ebi.atlas.download;

import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import java.util.Arrays;

public enum ExperimentFileType {
    // Could include icon name (similar to Description class in ExternallyAvailableContent)
    EXPERIMENT_DESIGN(
            "experiment-design", "Experiment design file (TSV format)", IconType.EXPERIMENT_DESIGN, false),
    SDRF(
            "sdrf", "SDRF file (.txt)", IconType.TSV, false),
    IDF(
            "idf", "IDF file (.txt)", IconType.TSV, false),
    CLUSTERING(
            "cluster", "Clustering file (TSV format)", IconType.TSV, false),
    QUANTIFICATION_RAW(
            "quantification-raw", "Raw counts files (MatrixMarket archive)", IconType.TSV, true),
    QUANTIFICATION_FILTERED(
            "quantification-filtered", "Filtered TPMs files (MatrixMarket archive)", IconType.TSV, true),
    NORMALISED(
            "normalised", "Normalised counts files (MatrixMarket archive)", IconType.TSV, true),
    MARKER_GENES(
            "marker-genes", "Marker gene files (TSV files archive)", IconType.TSV, true),
    HDF5(
            "hdf5", "HDF5", IconType.HDF5, false),
    EXPERIMENT_METADATA(
            "experiment-metadata", "Experiment metadata (SDRF and IDF files archive)", IconType.TSV, true);

    // IDs should be used when generating URLs
    private final String id;
    private final String description;
    private final IconType iconType;
    // Indicates if the file type has more than one associated files, which should be served as an archive
    private final boolean isArchive;

    ExperimentFileType(String id, String description, IconType iconType, boolean isArchive) {
        this.id = id;
        this.description = description;
        this.iconType = iconType;
        this.isArchive = isArchive;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public IconType getIconType() {
        return iconType;
    }

    public boolean isArchive() {
        return isArchive;
    }

    public static ExperimentFileType fromId(String id) {
        return Arrays.stream(ExperimentFileType.values())
                .filter(value -> value.id.equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException("No experiment file type with ID " + id + " was found"));
    }
}
