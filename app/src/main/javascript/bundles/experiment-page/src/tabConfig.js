
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
export const tabConfigurations = [
    { type: 'results', key: 'ks', component: TSnePlotViewRoute, optionsKey: 'plotTypesAndOptions' },
    { type: 'experiment-design', key: 'table.data', component: ExperimentDesignRoute },
    { type: 'supplementary-information', key: 'sections', component: SupplementaryInformationRoute },
    { type: 'resources', key: 'data', component: DownloadsRoute }
];