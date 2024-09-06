
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
    ['results', ['ks', 'defaultPlotMethodAndParameterisation', 'suggesterEndpoint']],
    ['cell-plots', ['metadata', 'defaultPlotMethodAndParameterisation', 'suggesterEndpoint']],
    ['marker-genes', ['ks', 'SelectedK']],
    ['anatomogram', ['searchTerm ', 'data']],
    ['experiment-design', ['table.headers.data']],
    ['supplementary-information', ['sections.props.data']],
    ['resources', ['data.files']]
]);

export const tabCommonValidations = new Map([
    ['results', ['atlasUrl ', 'experimentAccession']],
    ['cell-plots', ['atlasUrl ', 'experimentAccession']],
    ['marker-genes', ['host ', 'resources', 'species']],
    ['anatomogram', ['experimentAccession ', 'showIds', 'species', 'organ']],
    ['experiment-design', ['atlasUrl', 'experimentAccession']],
    ['supplementary-information', ['atlasUrl', 'experimentAccession']],
    ['resources', ['atlasUrl', 'experimentAccession']]
]);