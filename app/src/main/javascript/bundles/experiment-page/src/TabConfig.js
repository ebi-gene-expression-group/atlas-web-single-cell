
// Helper function to check if an object is empty
export const isObjectEmpty = (objectName) => {
    return (
        objectName &&
        Object.keys(objectName).length === 0 &&
        objectName.constructor === Object
    );
};

export const isEmptyArray = (value) => isObjectEmpty(value) && Array.isArray(value);

export const tabValidations = new Map([
    ['experiment-design', ['table.headers.data']],
    ['supplementary-information', ['sections.props.data']],
    ['resources', ['data.files']]
]);

export const innerTabValidations = new Map([
    ['Cell plots', ['ks', 'metadata', 'defaultPlotMethodAndParameterisation', 'suggesterEndpoint']],
    ['Marker Genes', ['ksWithMarkerGenes']],
]);

export const tabCommonValidations = new Map([
    ['experiment-design', ['atlasUrl', 'experimentAccession']],
    ['supplementary-information', ['atlasUrl', 'experimentAccession']],
    ['resources', ['atlasUrl', 'experimentAccession']]
]);