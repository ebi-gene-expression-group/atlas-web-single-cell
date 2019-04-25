package uk.ac.ebi.atlas.search;

public enum FacetType {
    INFERRED_CELL_TYPE("Inferred cell type", "Submitter-defined cell identity for a cell based on post-sequencing expression profile"),
    MARKER_GENE("Marker genes", "A gene that comprises part of the specific expression profile for that cluster"),
    ORGANISM_PART("Organism part", "The tissue from which the sample is originally derived, e.g. lung"),
    ORGANISM("Species", "");

    String tooltip;
    String title;


    FacetType(String title, String tooltip) {
        this.title = title;
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getTitle() {
        return title;
    }

    public static FacetType fromName(String name) {
        for (FacetType facetType : FacetType.values()) {
            if (facetType.name().equalsIgnoreCase(name)) {
                return facetType;
            }
        }
        return null;
    }
}
