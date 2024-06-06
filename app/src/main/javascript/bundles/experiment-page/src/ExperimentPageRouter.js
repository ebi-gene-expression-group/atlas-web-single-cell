import React from 'react'
import PropTypes from 'prop-types'
import {BrowserRouter, Route, Redirect, Switch, NavLink, withRouter} from 'react-router-dom'

import URI from 'urijs'

import TSnePlotViewRoute from './TSnePlotViewRoute'
import ExperimentDesignRoute from './ExperimentDesignRoute'
import SupplementaryInformationRoute from './SupplementaryInformationRoute'
import DownloadsRoute from './DownloadsRoute'

const RoutePropTypes = {
    match: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired
}

const TabCommonPropTypes = {
    atlasUrl: PropTypes.string.isRequired,
    experimentAccession: PropTypes.string.isRequired,
    species: PropTypes.string.isRequired,
    accessKey: PropTypes.string,
    resourcesUrl: PropTypes.string
}

// What component each tab type should render, coupled to ExperimentController.java
let tabTypeComponent = []

function isThisTabType(tab, tabType) {
    return tab.type === tabType;
}

const isObjectEmpty = (objectName) => {
    return (
        objectName &&
        Object.keys(objectName).length === 0 &&
        objectName.constructor === Object
    );
};

function enableExperimentPageTab(tab) {
    const resultTab = 'results'
    const experimentDesignTab = 'experiment-design'
    const supplementaryInformationTab = 'supplementary-information'
    const downloadTab = 'resources'
    console.log("in enableExperimentPageTab");
    if (isThisTabType(tab, resultTab)) {
        if (!isObjectEmpty(tab.props.ks) && Array.isArray(tab.props.ks) && !isObjectEmpty(tab.props.plotTypesAndOptions)) {
            tabTypeComponent.push({resultTab: TSnePlotViewRoute})
            return tab.name;
        }
    } else if (isThisTabType(tab, experimentDesignTab)) {
        if (!isObjectEmpty(tab.props.table.data) && Array.isArray(tab.props.table.data)) {
            tabTypeComponent.push({experimentDesignTab: ExperimentDesignRoute})
            return tab.name;
        }
    } else if (isThisTabType(tab, supplementaryInformationTab)) {
        if (!isObjectEmpty(tab.props.sections) && Array.isArray(tab.props.sections)) {
            tabTypeComponent.push({supplementaryInformationTab: SupplementaryInformationRoute})
            return tab.name;
        }
    } else if (isThisTabType(tab, downloadTab)) {
        if (!isObjectEmpty(tab.props.data) && Array.isArray(tab.props.data)) {
            tabTypeComponent.push({'resources': DownloadsRoute})
            return tab.name;
        }
    }
}

const TopRibbon = ({tabs, routeProps}) => {
    tabTypeComponent = [];
    return <ul className={`tabs`}>
        {
            tabs.map((tab) => (
                <li title={tab.name} key={tab.type} className={`tabs-title`}>
                    <NavLink to={{
                        pathname: `/${tab.type}`,
                        search: routeProps.location.search,
                        hash: routeProps.location.hash
                    }}
                             activeClassName={`active`}>
                        {enableExperimentPageTab(tab)}
                    </NavLink>
                </li>))
        }
    </ul>
}
TopRibbon.propTypes = {
    tabs: PropTypes.arrayOf(PropTypes.shape({
        type: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        props: PropTypes.object
    })).isRequired,
    routeProps: PropTypes.shape(RoutePropTypes)
}


const TabContent = ({type, tabProps, commonProps, routeProps}) => {
    // Pass in the search from location
    const Tab = tabTypeComponent[type]

    return (
        Tab ? <Tab {...tabProps} {...commonProps} {...routeProps}/> : null
    )
}

TabContent.propTypes = {
    type: PropTypes.string.isRequired,
    tabProps: PropTypes.object,
    commonProps: PropTypes.shape(TabCommonPropTypes),
    routeProps: PropTypes.shape(RoutePropTypes)
}

const RedirectWithSearchAndHash = (props) =>
    <Redirect to={{pathname: props.pathname, search: props.location.search, hash: props.location.hash}}/>

RedirectWithSearchAndHash.propTypes = {
    pathname: PropTypes.string.isRequired,
    location: PropTypes.shape({
        search: PropTypes.string.isRequired,
        hash: PropTypes.string.isRequired
    }).isRequired
}

const RedirectWithLocation = withRouter(RedirectWithSearchAndHash)

const ExperimentPageRouter = ({atlasUrl, resourcesUrl, experimentAccession, species, accessKey, tabs}) => {

    const tabCommonProps = {
        atlasUrl,
        resourcesUrl,
        experimentAccession,
        species,
        accessKey
    }
    console.log("in ExperimentPageRouter. Tabs length: " + tabs.length);
    return (
        <BrowserRouter
            basename={URI(`experiments/${experimentAccession}`, URI(atlasUrl).path()).toString()}>
            <div>
                <Route
                    path={`/`}
                    render={
                        (routeProps) =>
                            <TopRibbon
                                tabs={tabs}
                                routeProps={routeProps}/>
                    }/>
                <Switch>
                    {
                        tabs.map((tab) =>
                            <Route
                                key={tab.type}
                                path={`/${tab.type}`}
                                render={
                                    (routeProps) =>
                                        <TabContent
                                            type={tab.type}
                                            tabProps={tab.props}
                                            commonProps={tabCommonProps}
                                            routeProps={routeProps}/>
                                }/>
                        )
                    }
                    <RedirectWithLocation pathname={`/${tabs[0].type}`}/>
                </Switch>
            </div>
        </BrowserRouter>
    )
}

ExperimentPageRouter.propTypes = {
    ...TabCommonPropTypes,
    tabs: PropTypes.arrayOf(PropTypes.shape({
        type: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        props: PropTypes.object.isRequired
    })).isRequired
}

export default ExperimentPageRouter
