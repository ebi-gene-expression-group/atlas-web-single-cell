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
    let selectedAccessionOrLinkAccession = ids
    if (ids[0] === `link_islet_of_langerhans`) {
      selectedAccessionOrLinkAccession = [`UBERON_0001264`]
    } else if (ids[0] === `link_glomerulus`) {
      selectedAccessionOrLinkAccession = [`UBERON_0000074`]
    } else if (ids[0] === `link_duct`) {
      selectedAccessionOrLinkAccession = [`UBERON_0001232`]
    } else if (ids[0] === `link_nephron`) {
      selectedAccessionOrLinkAccession = [`UBERON_0001285`]
    } else if (ids[0] === `link_lobule_of_liver`) {
      selectedAccessionOrLinkAccession = [ // `UBERON_0004647`,
        `CL_0000182`, `CL_0000632`, `UBERON_0001282`, `CL_1000488`, `UBERON_0001193`, `UBERON_0006841`,
        `UBERON_0001639`, `EFO_0010704`, `UBERON_0001281`, `CL_1000398`, `EFO_0010705`, `EFO_0010706`, `CL_0000091`
      ]
    } else if (ids[0] === `link_alveoli`) {
      selectedAccessionOrLinkAccession = [
        `UBERON_0002299`, `UBERON_0002188`, `UBERON_0016405`
      ]
    } else if (ids[0] === `link_segmental_bronchus`) {
      selectedAccessionOrLinkAccession = [ //`UBERON_0002184`,
        `CL_0002633`, `CL_1001567`,  `CL_1000271`, `CL_0000158`, `EFO_0010666`, `CL_0002370`, `UBERON_0003504`,
        `CL_0002598`
      ]
    } else if (ids[0] === `link_alveolus_section`) {
      selectedAccessionOrLinkAccession = [ // `UBERON_0003215`,
        `CL_0002062`, `CL_0002063`, `UBERON_0003456`, `EFO_0010667`, `EFO_0010668`, `EFO_0010669`
      ]
    } else if (ids[0] === `link_cell_level`) {
      selectedAccessionOrLinkAccession = [
        `CL_2000002`, `EFO_0010710`, `CL_3000001`, `UBERON_0000371`, `UBERON_0000319`, `UBERON_0000426`,
        `EFO_0010712`, `EFO_0010711`, `CL_0002601`, `EFO_0010708`
      ]
    }

    this.setState({
      selectIds: ids,
      resource:
        URI(`json/experiments/${this.props.experimentAccession}/marker-genes/cell-types`, this.props.host)
          .search({organismPart: selectedAccessionOrLinkAccession.map(ontologyAccessionToOntologyUri).join(`,`)})
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
