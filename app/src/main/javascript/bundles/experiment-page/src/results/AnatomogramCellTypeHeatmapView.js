import React from 'react'
import PropTypes from 'prop-types'
import URI from 'urijs'

import Anatomogram from '@ebi-gene-expression-group/organ-anatomogram'
import HeatmapView from '@ebi-gene-expression-group/scxa-marker-gene-heatmap'
import ontologyIds from './ontologyIds.json'

const ontologyAccessionToOntologyUri = (accession) => {
  return accession.startsWith(`EFO`) ?
    `http://www.ebi.ac.uk/efo/${accession}` :
    `http://purl.obolibrary.org/obo/${accession}`
}

class AnatomogramCellTypeHeatmapView extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      selectedSpecies: props.organ,
      showIds: props.showIds,
      highlightIds: [],
      selectIds: [],
      selectAllIds: [],
      resource:
          URI(`json/experiments/${props.experimentAccession}/marker-genes/cell-types`, props.host)
              .search({organismPart: ontologyAccessionToOntologyUri(ontologyIds[props.organ][props.organ][0])})
              .toString()
    }

    this._addRemoveFromSelectIds = this._addRemoveFromSelectIds.bind(this)
    this._showLinkBoxIds = this._showLinkBoxIds.bind(this)
    this._handleSelectOnChange = this._handleSelectOnChange.bind(this)
    this._afterSwitchView = this._afterSwitchView.bind(this)
  }

  _handleSelectOnChange(event) {
    this.setState({
      selectedSpecies: event.target.value
    })
  }

  _addRemoveFromSelectIds(ids) {
    if (ids.length === 0 || ids[0].startsWith(`link`)) {
      this.setState({
        selectIds: ids
      })
    } else {
      this.setState({
        selectIds: ids,
        resource:
            URI(`json/experiments/${this.props.experimentAccession}/marker-genes/cell-types`, this.props.host)
                .search({organismPart: ids.map(ontologyAccessionToOntologyUri).join(`,`)})
                .toString()
      })
    }
  }

  _afterSwitchView(species, view) {
    let requestOntologyIds = []

    if (ontologyIds[species] && ontologyIds[species][view]) {
      requestOntologyIds = ontologyIds[species][view]
    }

  this.setState({
       resource:
         URI(`json/experiments/${this.props.experimentAccession}/marker-genes/cell-types`, this.props.host)
           .search({ organismPart: requestOntologyIds.map(ontologyAccessionToOntologyUri).join(`,`) })
           .toString()
    })
  }

  _showLinkBoxIds(id) {
    this.setState({
      showIds: [...new Set(id.concat(this.state.showIds))]
    })
  }

  render() {
    const {host} = this.props

    const heatmapArgs = {
      host: host,
      resource: this.state.resource,
      species: `Homo sapiens`,
      hasDynamicHeight: true,
      heatmapRowHeight: 20,
      wrapperClassName: `row expanded`,
      heatmapType: 'celltypes'
    }

    return (
      <div className={`row expanded`}>
        <div className={`small-12 medium-6 columns`}>
          <Anatomogram
            species={this.state.selectedSpecies}
            showIds={this.state.showIds}
            highlightIds={this.state.highlightIds}
            selectIds={this.state.selectIds}
            onClick={this._addRemoveFromSelectIds}
            selectAllIds={[]}
            showLinkBoxIds={this._showLinkBoxIds}
            afterSwitchView={this._afterSwitchView}
          />
        </div>

        <div className={`small-12 medium-6 columns`}>
          <HeatmapView {...heatmapArgs} />
        </div>
      </div>
    )
  }
}

AnatomogramCellTypeHeatmapView.propTypes = {
  host: PropTypes.string,
  resource: PropTypes.string,
  experimentAccession: PropTypes.string.isRequired,
  showIds: PropTypes.array.isRequired,
  species: PropTypes.string.isRequired,
  organ: PropTypes.string.isRequired
}

export default AnatomogramCellTypeHeatmapView
