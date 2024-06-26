
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
export const getNestedProperty = (obj, path) => path.split('.').reduce((acc, key) => acc?.[key], obj);

// Helper function to check if a value is a non-empty array
export const isNonEmptyArray = (value) => !isObjectEmpty(value) && Array.isArray(value);

// Configuration for tab types, their keys, and components
export const tabValidations = [
    { type: 'results', requiredProps: ['ks ','selectedPlotOption','selectedPlotType','suggesterEndpoint'], component: TSnePlotViewRoute},
    { type: 'cell-plots', requiredProps: ['ks ','selectedPlotOption','selectedPlotType','suggesterEndpoint'], component: TSnePlotViewRoute},
    { type: 'marker-genes', requiredProps: ['Ks','SelectedK'], component: TSnePlotViewRoute},
    { type: 'anatomogram', requiredProps: ['searchTerm ','data'], component: TSnePlotViewRoute},
    { type: 'experiment-design', requiredProps: ['table','data','headers'], component: ExperimentDesignRoute },
    { type: 'supplementary-information', requiredProps: ['sections','data','type'], component: SupplementaryInformationRoute },
    { type: 'resources', requiredProps: ['data','url'], component: DownloadsRoute }
];

export const tabCommonValidations = [
    { type: 'results', commonValidations: ['atlasUrl ','experimentAccession'], component: TSnePlotViewRoute},
    { type: 'cell-plots', commonValidations: ['atlasUrl ','experimentAccession'], component: TSnePlotViewRoute},
    { type: 'marker-genes', commonValidations: ['host ','resources','species'], component: TSnePlotViewRoute},
    { type: 'anatomogram', commonValidations: ['experimentAccession ','showIds','species','organ'], component: TSnePlotViewRoute},
    { type: 'experiment-design', commonValidations: ['atlasUrl','downloadUrl','experimentAccession'], component: ExperimentDesignRoute },
    { type: 'supplementary-information', commonValidations: ['atlasUrl','experimentAccession'], component: SupplementaryInformationRoute },
    { type: 'resources', commonValidations: ['atlasUrl','experimentAccession'], component: DownloadsRoute }
];