import React from 'react'
import PropTypes from 'prop-types'
import URI from 'urijs'
import {BrowserRouter, Route, NavLink, Switch, Redirect, withRouter} from 'react-router-dom'

//import TSnePlotView from '@ebi-gene-expression-group/scxa-tsne-plot'
import HeatmapView from 'scxa-marker-gene-heatmap'
import BioentityInformation from '@ebi-gene-expression-group/atlas-bioentity-information'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const BioentityInformationWithFetchLoader = withFetchLoader(BioentityInformation)

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

class TSnePlotViewRoute extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    const {location, match, history} = this.props
    const {atlasUrl, suggesterEndpoint} = this.props
    const {species, experimentAccession, ks, ksWithMarkerGenes, perplexities, metadata} = this.props
    const search = URI(location.search).search(true)

    const routes = [
      {
        path: `/tsne`,
        title: `t-SNE plots`,
        main: () => null
      },
      {
        path: `/marker-genes`,
        title: `Marker Genes`,
        main: () => <HeatmapView
          host={atlasUrl}
          resource={`json/experiments/${experimentAccession}/marker-genes/${search.markerGeneK || preferredK}`}
          wrapperClassName={`row expanded`}
          ks={ks}
          selectedK={search.markerGeneK || preferredK}
          onSelectK={
            (k) => {
              const query = new URLSearchParams(history.location.search)
              query.set(`markerGeneK`, k)
              // If tsne plot is coloured by k
              if (!query.has(`metadata`)) {
                query.set(`k`, k)
                query.set(`colourBy`, `clusters`)
              }
              resetHighlightClusters(query)
              updateUrlWithParams(query)
            }
          }
          ksWithMarkers={ksWithMarkerGenes}
        />
      },
      {
        path: `/gene-info`,
        title: `Gene information`,
        main: () =>
          <BioentityInformationWithFetchLoader
            host={atlasUrl}
            resource={`json/bioentity-information/${search.geneId}`}/>
      }
    ]

    const updateUrlWithParams = (query) => {
      history.push({...history.location, search: query.toString()})
    }

    const resetHighlightClusters = (query) => {
      if(query.has(`clusterId`)) {
        query.delete(`clusterId`)
      }
    }

    // Sort perplexities in ascending order
    const perplexitiesOrdered = perplexities.sort((a, b) => a - b)

    const preferredK = this.props.selectedK ? this.props.selectedK.toString() : this.props.ks[0].toString()

    const basename = URI(`experiments/${experimentAccession}${match.path}`, URI(atlasUrl).path()).toString()

    return (
      <BrowserRouter basename={basename}>
        <div className={`row expanded`}>
          <div
            className={`small-3 medium-2 large-1 columns`}
            style={{
              padding: 0,
              background: `#ffffff`
            }}>
            <ul className={`side-tabs`}>
              <li title={routes[0].title} className={`side-tabs-title`}>
                <NavLink to={{pathname:routes[0].path, search: location.search, hash: location.hash}}
                  activeClassName={`active`}>
                  {routes[0].title}</NavLink>
              </li>
              <li title={routes[1].title} className={`side-tabs-title`}>
                <NavLink to={{pathname:routes[1].path, search: location.search, hash: location.hash}}
                  activeClassName={`active`}>
                  {routes[1].title}</NavLink>
              </li>
              {
                search.geneId &&
                  <li title={routes[2].title} className={`side-tabs-title`}>
                    <NavLink to={{pathname:routes[2].path, search: location.search, hash: location.hash}}
                      activeClassName={`active`}>
                      {routes[2].title}</NavLink>
                  </li>
              }
            </ul>
          </div>
          <div
            className={`small-9 medium-10 large-11 columns`}
            style={{padding: `10px`}}>
            <Switch>
              {routes.map((route, index) => (
                <Route
                  key={index}
                  path={route.path}
                  component={route.main}
                />
              ))}
              <RedirectWithLocation pathname={`${routes[0].path}`} />
            </Switch>
          </div>
        </div>
      </BrowserRouter>
    )
  }
}

TSnePlotViewRoute.propTypes = {
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  history: PropTypes.object.isRequired,
  atlasUrl: PropTypes.string.isRequired,
  resourcesUrl: PropTypes.string,
  experimentAccession: PropTypes.string.isRequired,
  ks: PropTypes.arrayOf(PropTypes.number).isRequired,
  ksWithMarkerGenes: PropTypes.arrayOf(PropTypes.number).isRequired,
  perplexities: PropTypes.arrayOf(PropTypes.number).isRequired,
  suggesterEndpoint: PropTypes.string.isRequired,
  species: PropTypes.string.isRequired,
  metadata: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired
  }).isRequired).isRequired,
  selectedK: PropTypes.number
}

export default TSnePlotViewRoute
