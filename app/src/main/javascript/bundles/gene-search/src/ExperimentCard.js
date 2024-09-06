import React from 'react'
import PropTypes from 'prop-types'
import formatNumber from 'format-number'
import EbiSpeciesIcon from '@ebi-gene-expression-group/react-ebi-species'
import {ExperimentIconDiv, CardContainerDiv, SmallIconDiv, IconDiv, MarkerDiv, TitleDiv, VariableDiv, CountDiv}from './ExperimentCardDiv'

const _formatNumber = formatNumber()

const ANNDATA = `E-ANND`

class ExperimentCard extends React.Component {
  constructor(props) {
    super(props)
  }

  _goToExperiment(url) {
    window.location = url
  }

  render() {
    const {url, species, experimentDescription, markerGenes, numberOfAssays, experimentAccession, type, factors} = this.props

    const markerGeneLinks = markerGenes ? markerGenes.map((markerGene) =>
      <li key={`marker-gene-${markerGene.k}`}>
        <a href={markerGene.url}>See cluster {markerGene.clusterIds.sort().join(`, `)} for k = {markerGene.k}</a>
      </li>) :
      []

    const showIcon = experimentAccession.startsWith(ANNDATA)

    return (
      <CardContainerDiv onClick={this._goToExperiment.bind(this, url)}>
        <IconDiv>
          <EbiSpeciesIcon species={species}/>
          <h6>{species}</h6>
        </IconDiv>
        {
          markerGeneLinks.length ?
            <MarkerDiv>
              <ul style={{marginBottom: 0}}>
                {markerGeneLinks}
              </ul>
            </MarkerDiv> :
            // Be aware that the FacetedSearchContainer in the search results component will insert <ReactTooltip/>
            <MarkerDiv>
              <span
                data-tip={`<span>Not a marker gene</span>`}
                data-html={true}
                className={`icon icon-functional`}
                data-icon={`x`} />
            </MarkerDiv>
        }
       <TitleDiv>
           {experimentDescription}
            <SmallIconDiv>
              {showIcon &&
                <a href={`${window.location.host}/gxa/sc/help.html?section=external-data`}>
                  <ExperimentIconDiv background={`indianred`} color={`white`} data-toggle={`tooltip`}
                                     data-placement={`bottom`} title={`Externally analysed data`}
                  >E</ExperimentIconDiv>
                </a>}
            </SmallIconDiv>
       </TitleDiv>
        <VariableDiv>
          <ul style={{marginBottom: 0}}>
              {factors.map(factor => <li key={`factor-${factor}`}> {factor} </li>)}
          </ul>
        </VariableDiv>
      <CountDiv> {_formatNumber(numberOfAssays)} </CountDiv>
      </CardContainerDiv>
    )
  }
}

ExperimentCard.propTypes = {
    url: PropTypes.string.isRequired,
    species: PropTypes.string.isRequired,
    experimentDescription: PropTypes.string.isRequired,
    markerGenes: PropTypes.arrayOf(PropTypes.shape({
    k: PropTypes.number.isRequired,
    clusterIds: PropTypes.array.isRequired,
    url: PropTypes.string.isRequired
  })),
  numberOfAssays: PropTypes.number.isRequired,
  factors: PropTypes.array.isRequired,
  experimentAccession: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired
}

export default ExperimentCard
