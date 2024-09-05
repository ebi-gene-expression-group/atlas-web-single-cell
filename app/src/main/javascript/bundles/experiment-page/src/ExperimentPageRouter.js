import React from 'react'
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

function shouldRender(tab, commonProps){
    var shouldRender = true;
    var commonRequiredProps = tabCommonValidations.get(tab.type);

    if(commonRequiredProps != null){
        commonRequiredProps.forEach(commonProp=> {
            var propValue = commonProps.valueOf(commonProp);
            if(propValue==='undefined' || propValue=='' || propValue==null) {
             console.log(tab.type +" data missing the required value for the attribute "+commonProp);
                shouldRender = false;
                return false;
            }
        })
    }

    var requiredProps = tabValidations.get(tab.type);
    var tabProps = tab.props;
    if(requiredProps != null){
        requiredProps.forEach(requiredProp=>{
            var propValue = tabProps[requiredProp];
            if(propValue==='undefined' || propValue=='' || propValue==null) {
                console.log(tab.type +" data missing the required value for the attribute "+requiredProp);
                shouldRender = false;
                return false;
            }
            if(requiredProp == 'ks'){
                if(propValue.length==0){
                    console.log(tab.type +" ks array length is 0");
                    shouldRender = false;
                    return false;
                }
            }
             if(requiredProp == 'defaultPlotMethodAndParameterisation') {
                if(isEmptyArray(propValue)){
                    console.log(tab.type +" selectedPlotOption and selectedPlotType doesn't have data");
                    shouldRender = false;
                    return false;
                }
            }
             if (requiredProp == 'suggesterEndpoint') {
                 if(propValue.length == 0) {
                     console.log(tab.type +" suggesterEndpoint doesn't have data")
                     shouldRender = false;
                     return false;
                 }
             }
        });
    }
    console.log(tab.type +" data validation pass. Returning "+shouldRender);

    return shouldRender;
}

const TopRibbon = ({tabs, routeProps, commonProps}) =>
    <ul className={`tabs`}>
        {
            tabs.map((tab) => {
                 if(shouldRender(tab, commonProps) === true) {
                     console.log("rendering tab");
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
                                                routeProps={routeProps}/>
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