import React from 'react'
import PropTypes from 'prop-types'
import URI from 'urijs'

import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'
import GeneSearchForm from '@ebi-gene-expression-group/scxa-gene-search-form'
import FacetedSearchResults from '@ebi-gene-expression-group/scxa-faceted-search-results'

import ExperimentsHeader from './ExperimentsHeader'
import ExperimentCard from './ExperimentCard'

const GeneSearchFormWithFetchLoader = withFetchLoader(GeneSearchForm)
const FacetedSearchResultsWithFetchLoader = withFetchLoader(FacetedSearchResults)

const GeneSearch = ({atlasUrl, history}) => {
  const updateUrl = (event, query, species) => {
    event.preventDefault()
    history.push(`/search?species=` + species + `&` + query.category + `=` + query.term)
  }

  const requestParams = URI(location.search).query(true)

  // From BioentitiesCollectionProxy.java // ID_PROPERTY_NAMES
  const idPropertyNames =
    [`q`, `ensgene`, `symbol`, `entrezgene`, `hgnc_symbol`, `mgi_id`, `mgi_symbol`, `flybase_gene_id`, `wbpsgene`]

  // The gene query will be the first non-empty request param key from idPropertyNames
  const firstMatchingRequestParam =
    Object.keys(requestParams)
      .find(requestParamKey => idPropertyNames.includes(requestParamKey))

  const geneQuery = firstMatchingRequestParam ?
    {
      term: requestParams[firstMatchingRequestParam],
      category: firstMatchingRequestParam
    } :
    {}

  return (
    <div>
      <GeneSearchFormWithFetchLoader
        host={atlasUrl}
        resource={`json/suggestions/species`}
        wrapperClassName={`row expanded`}
        actionEndpoint={`search`}
        onSubmit={updateUrl}
        autocompleteClassName={`small-8 columns`}
        suggesterEndpoint={`json/suggestions/gene_ids`}
        defaultValue={geneQuery}
        enableSpeciesSelect={true}
        speciesSelectClassName={`small-4 columns`}
        defaultSpecies={requestParams.species} />

      {
        firstMatchingRequestParam &&
        <FacetedSearchResultsWithFetchLoader
          host={atlasUrl}
          resource={`json/search`}
          query={{
            [geneQuery.category]: geneQuery.term,
            species: requestParams.species
          }}
          fulfilledPayloadProvider={data => ({
            resultsMessage: data ?
              (data.results && data.results.length > 0) ?
                `${geneQuery.term} ${data.matchingGeneId}  is expressed in:` :
                `Your search for ${geneQuery.term} yielded no results: ${data.reason}` :
              `WTF!`
            })
          }
          sortTitle={`markerGenes`}
          ResultsHeaderClass={ExperimentsHeader}
          ResultElementClass={ExperimentCard}
        />
      }
    </div>
  )
}

GeneSearch.propTypes = {
  atlasUrl: PropTypes.string.isRequired,
  history: PropTypes.object.isRequired
  // If we really need to know history’s propTypes (e.g. for tests) here they are:
  // history: PropTypes.shape({
  //   length: PropTypes.number.isRequired,
  //   action: PropTypes.oneOf([`POP`, `PUSH`, `REPLACE`]).isRequired,
  //   location: PropTypes.shape({
  //     hash: PropTypes.string.isRequired,
  //     key: PropTypes.string.isRequired,
  //     query: PropTypes.string,
  //     state: PropTypes.string
  //   }).isRequired,
  //   createHref: PropTypes.func.isRequired,
  //   push: PropTypes.func.isRequired,
  //   replace: PropTypes.func.isRequired,
  //   go: PropTypes.func.isRequired,
  //   goBack: PropTypes.func.isRequired,
  //   goForward: PropTypes.func.isRequired,
  //   block: PropTypes.func.isRequired,
  //   listen: PropTypes.func.isRequired
  // })
}

export default GeneSearch
