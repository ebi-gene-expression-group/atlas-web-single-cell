import PropTypes from "prop-types";

export const RoutePropTypes = {
    match: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired
}
export const TabCommonPropTypes = {
    atlasUrl: PropTypes.string.isRequired,
    experimentAccession: PropTypes.string.isRequired,
    species: PropTypes.string.isRequired,
    accessKey: PropTypes.string,
    resourcesUrl: PropTypes.string
};