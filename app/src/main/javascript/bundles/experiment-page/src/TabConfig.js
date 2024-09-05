
import TSnePlotViewRoute from './TSnePlotViewRoute';
import ExperimentDesignRoute from './ExperimentDesignRoute';
import SupplementaryInformationRoute from './SupplementaryInformationRoute';
import DownloadsRoute from './DownloadsRoute';

// Helper function to check if the tab type matches
export function isThisTabType(tab, tabType) {
    return tab.type === tabType;
}

// Helper function to check if an object is empty
export const isObjectEmpty = (objectName) => {
    return (
        objectName &&
        Object.keys(objectName).length === 0 &&
        objectName.constructor === Object
    );
};

// Helper function to get the value of a nested property given a path
export const getNestedProperty = (obj, path) => path.keys((acc, key) => acc?.[key], obj);

// Helper function to check if a value is a non-empty array
export const isEmptyArray = (value) => isObjectEmpty(value) && Array.isArray(value);

export const tabValidations = new Map([
    [  'results',  ['ks','defaultPlotMethodAndParameterisation', 'suggesterEndpoint']],
    [  'cell-plots',['ks','defaultPlotMethodAndParameterisation', 'suggesterEndpoint']],
    [  'marker-genes', ['ks','SelectedK']],
    [ 'anatomogram',  ['searchTerm ','data']],
    [  'experiment-design',  ['table.headers.data']],
    [ 'supplementary-information', ['sections','data','type']],
    [  'resources', ['data','url']]
]);

export const tabCommonValidations = new Map([
    [ 'results',  ['atlasUrl ','experimentAccession'] ],
    [ 'cell-plots', ['atlasUrl ','experimentAccession'] ],
    [ 'marker-genes',  ['host ','resources','species'] ],
    [ 'anatomogram', ['experimentAccession ','showIds','species','organ'] ],
    [ 'experiment-design',  ['atlasUrl','downloadUrl','experimentAccession'] ],
    [ 'supplementary-information', ['atlasUrl','experimentAccession'] ],
    [ 'resources', ['atlasUrl','experimentAccession'] ]
]);