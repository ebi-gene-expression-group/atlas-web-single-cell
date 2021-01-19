import React from 'react'
import PropTypes from 'prop-types'
import URI from 'urijs'

import Anatomogram from '@ebi-gene-expression-group/organ-anatomogram'
import HeatmapView from '@ebi-gene-expression-group/scxa-marker-gene-heatmap'

const supportedOrgans = {
  pancreas: `UBERON_0001264`,
  kidney: `UBERON_0002113`,
  liver: `UBERON_0002107`,
  placenta: `UBERON_0001987`,
  lung: `UBERON_0002048`
}

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
          .search({organismPart: ontologyAccessionToOntologyUri(supportedOrgans[props.organ])})
          .toString()
    }

    this._addRemoveFromSelectIds = this._addRemoveFromSelectIds.bind(this)
    this._showLinkBoxIds = this._showLinkBoxIds.bind(this)
    this._handleSelectOnChange = this._handleSelectOnChange.bind(this)
  }

  _handleSelectOnChange(event) {
    this.setState({
      selectedSpecies: event.target.value
    })
  }

  _addRemoveFromSelectIds(ids) {
    let selectedAccessionOrLinkAccession = ids[0]
    if (ids[0] === `link_islet_of_langerhans`) {
      selectedAccessionOrLinkAccession = `UBERON_0001264`
    } else if (ids[0] === `link_glomerulus`) {
      selectedAccessionOrLinkAccession = `UBERON_0000074`
    } else if (ids[0] === `link_duct`) {
      selectedAccessionOrLinkAccession = `UBERON_0001232`
    } else if (ids[0] === `link_nephron`) {
      selectedAccessionOrLinkAccession = `UBERON_0001285`
    } else if (ids[0] === `link_lobule_of_liver`) {
      selectedAccessionOrLinkAccession = `UBERON_0004647`
    } else if (ids[0] === `link_alveoli`) {
      selectedAccessionOrLinkAccession = `UBERON_0002299`
    } else if (ids[0] === `link_segmental_bronchus`) {
      selectedAccessionOrLinkAccession = `UBERON_0002184`
    } else if (ids[0] === `link_alveolus_section`) {
      selectedAccessionOrLinkAccession = `UBERON_0003215`
    } else if (ids[0] === `link_cell_level`) {
      selectedAccessionOrLinkAccession = `UBERON_0001987`
    }

    this.setState({
      selectIds: ids,
      resource:
        URI(`json/experiments/${this.props.experimentAccession}/marker-genes/cell-types`, this.props.host)
          .search({organismPart: ontologyAccessionToOntologyUri(selectedAccessionOrLinkAccession)})
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
