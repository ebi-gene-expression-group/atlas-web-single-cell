import React, { useState } from 'react'
import PropTypes from 'prop-types'
import { BrowserRouter, Route, Redirect, Switch, NavLink, withRouter } from 'react-router-dom'

import URI from 'urijs'

import TSnePlotViewRoute from './TSnePlotViewRoute'
import ExperimentDesignRoute from './ExperimentDesignRoute'
import SupplementaryInformationRoute from './SupplementaryInformationRoute'
import DownloadsRoute from './DownloadsRoute'
import {isEmptyArray, tabCommonValidations, tabValidations} from "./TabConfig";

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
const tabTypeComponent = {
    'results' : TSnePlotViewRoute,
    'experiment-design' : ExperimentDesignRoute,
    'supplementary-information' : SupplementaryInformationRoute,
    'downloads' : DownloadsRoute
}

function shouldRender(tab, commonProps) {
    let shouldRender = true;
    const commonRequiredProps = tabCommonValidations.get(tab.type);

    if (commonRequiredProps != null) {
        commonRequiredProps.some(commonProp => {
            const propValue = commonProps.valueOf(commonProp);
            if (propValue === 'undefined' || propValue == '' || propValue == null) {
                console.log(`${tab.type} data missing the required value for the attribute ${commonProp}`);
                shouldRender = false;
                return false;
            }
        })
    }

    const requiredProps = tabValidations.get(tab.type);
    const tabProps = tab.props;

    if (requiredProps != null) {
        requiredProps.forEach(requiredProp => {
            // Check if property requires nested object validation
            if (requiredProp.includes('.')) {
                const splitProps = requiredProp.split('.');
                splitProps.forEach(splitProp => {

                    let table = [];
                    let tableHeader = [];
                    let TableData = [];

                    if (isEmptyArray(table)) {
                        table = tabProps[splitProp] || [];
                        if (table.length == 0) {
                            console.log(`${tab.type}: table doesn't have data`);
                            shouldRender = false;
                            return false; // Early return on failure
                        }
                    }
                    if (isEmptyArray(tableHeader)) {
                        tableHeader = table[splitProp] || [];
                        if (tableHeader.length == 0) {
                            console.log(tab.type + ":" + " table headers doesn't have data")
                            shouldRender = false;
                            return false;
                        }
                    }

                    if (isEmptyArray(TableData)) {
                        TableData = tableHeader[splitProp]
                        if (TableData.length == 0) {
                            console.log(tab.type + ":" + " table table data doesn't have")
                            shouldRender = false;
                            return false;
                        }
                    }
                });
                return shouldRender;
            }

            var propValue = tabProps[requiredProp];
            if (propValue === 'undefined' || propValue == '' || propValue == null) {
                console.log(tab.type + " data missing the required value for the attribute " + requiredProp);
                shouldRender = false;
                return false;
            }
        });
    }
    console.log(tab.type + " data validation pass. Returning " + shouldRender);

    return shouldRender;
}

const TopRibbon = ({tabs, routeProps, commonProps}) =>
    <ul className={`tabs`}>
        {
            tabs.map((tab) => {
                 if(shouldRender(tab, commonProps) === true) {
                     console.log("rendering tab"+tab.type);
                     return <li title={tab.name} key={tab.type} className={`tabs-title`}>
                         <NavLink to={{
                             pathname: `/${tab.type}`,
                             search: routeProps.location.search,
                             hash: routeProps.location.hash
                         }}
                                  activeClassName={`active`}>
                             {tab.name}
                         </NavLink>
                     </li>
                 }
                }
            )}
    </ul>

TopRibbon.propTypes = {
    tabs: PropTypes.arrayOf(PropTypes.shape({
        type: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        props: PropTypes.object
    })).isRequired,
    routeProps: PropTypes.shape(RoutePropTypes)
}


const TabContent = ({type, tabProps, commonProps, routeProps, resultTabView}) => {
    // Pass in the search from location
    const Tab = tabTypeComponent[type]

    return (
        Tab ? <Tab {...tabProps} {...commonProps} {...routeProps} enableView={resultTabView} /> : null
    )
}

TabContent.propTypes = {
    type: PropTypes.string.isRequired,
    tabProps: PropTypes.object,
    commonProps: PropTypes.shape(TabCommonPropTypes),
    routeProps: PropTypes.shape(RoutePropTypes),
    resultTabView : PropTypes.func
}

const RedirectWithSearchAndHash = (props) =>
    <Redirect to={{ pathname: props.pathname, search: props.location.search, hash: props.location.hash}} />

RedirectWithSearchAndHash.propTypes = {
    pathname: PropTypes.string.isRequired,
    location: PropTypes.shape({
        search: PropTypes.string.isRequired,
        hash: PropTypes.string.isRequired
    }).isRequired
}

const RedirectWithLocation = withRouter(RedirectWithSearchAndHash)

const ExperimentPageRouter = ({atlasUrl, resourcesUrl, experimentAccession, species, accessKey, tabs}) => {

    const [enableResultTab, setEnableResultTab] = useState(false)



    let resultTabView;
    resultTabView = (resultTabView) => {
        if(resultTabView) {
            setEnableResultTab(true);
        }
    }

    const tabCommonProps = {
        atlasUrl,
        resourcesUrl,
        experimentAccession,
        species,
        accessKey
    }

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
                                routeProps={routeProps}
                                commonProps={tabCommonProps}
                            />
                    } />
                <Switch>
                    {
                        tabs.map((tab) =>
                        {
                            if(shouldRender(tab, tabCommonProps)) {
                               return <Route
                                    key={tab.type}
                                    path={`/${tab.type}`}
                                    render={
                                        (routeProps) =>
                                            <TabContent
                                                type={tab.type}
                                                tabProps={tab.props}
                                                commonProps={tabCommonProps}
                                                routeProps={routeProps}
                                                resultTabView={resultTabView}
                                            />
                                    }/>
                            }
                    })
                    }
                    <RedirectWithLocation pathname={`/${tabs[0].type}`} />
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