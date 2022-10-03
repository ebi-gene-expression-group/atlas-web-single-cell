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
  lung: `UBERON_0002048`,
  gut: `UBERON_0000160`
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
    if (species === `kidney`) {
      if (view === `kidney`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `nephron`) {
        requestOntologyIds = [`UBERON_0001285`]
      } else if (view === `glomerulus`) {
        requestOntologyIds = [`UBERON_0000074`]
      } else if (view === `duct`) {
        requestOntologyIds = [`UBERON_0001232`]
      }
    } else if (species === `liver`) {
      if (view === `liver`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `lobule`) {
        requestOntologyIds = [ // `UBERON_0004647`,
          `CL_0000182`, `CL_0000632`, `UBERON_0001282`, `CL_1000488`, `UBERON_0001193`, `UBERON_0006841`,
          `UBERON_0001639`, `EFO_0010704`, `UBERON_0001281`, `CL_1000398`, `EFO_0010705`, `EFO_0010706`, `CL_0000091`
        ]
      }
    } else if (species === `lung`) {
      if (view === `lung`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `airway`) {
        requestOntologyIds = [ // `UBERON_0002184`,
          `CL_0002633`, `CL_1001567`,  `CL_1000271`, `CL_0000158`, `EFO_0010666`, `CL_0002370`, `UBERON_0003504`,
          `CL_0002598`
        ]
      } else if (view === `alveoli_fin`) {
        requestOntologyIds = [ `UBERON_0002299`, `UBERON_0002188`, `UBERON_0016405` ]
      } else if (view === `alveoli_section`) {
        requestOntologyIds = [ // `UBERON_0003215`,
          `CL_0002062`, `CL_0002063`, `UBERON_0003456`, `EFO_0010667`, `EFO_0010668`, `EFO_0010669`
        ]
      }
    } else if (species === `pancreas`) {
      if (view === `pancreas`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `acinus`) {
        requestOntologyIds = [
          `CL_0000173`, `CL_0000171`, `CL_0000169`, `CL_0002275`, `CL_0005019`, `CL_0002410`, `CL_0000622`
        ]
      }
    } else if (species === `placenta`) {
      if (view === `placenta`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `cells`) {
        requestOntologyIds = [
          `CL_2000002`, `EFO_0010710`, `CL_3000001`, `UBERON_0000371`, `UBERON_0000319`, `UBERON_0000426`,
          `EFO_0010712`, `EFO_0010711`, `CL_0002601`, `EFO_0010708`
        ]
      }
    } else if (species === `gut`) {
      if (view === `gut`) {
        requestOntologyIds = [supportedOrgans[species]]
      } else if (view === `colon`) {
        requestOntologyIds = [
          "UBERON_8410051", "UBERON_8410015", "UBERON_8410048", "UBERON_8410057",
          "UBERON_0007178", "UBERON_8410000", "UBERON_8410061", "UBERON_8410058", "UBERON_8410059",
          "CL_0009043", "CL_0009042", "CL_0009041", "CL_1000347", "CL_0009039", "CL_0009011",
          "CL_0009009", "CL_0009025", "CL_0009038", "CL_0009040", "UBERON_8410060"
        ]
      } else if (view === `intestine`) {
        requestOntologyIds = [
          "CL_1000275", "CL_1000334", "UBERON_8410002", "UBERON_0018410", "UBERON_8410004",
          "UBERON_8410001", "UBERON_8410063", "UBERON_8410064", "UBERON_0001210", "UBERON_0012401",
          "UBERON_0012402", "CL_0009012", "CL_1000495", "CL_1000343", "CL_1000353", "CL_0009080",
          "CL_0009017", "CL_0009006", "CL_0009024", "CL_0009022", "CL_1000411", "CL_0009015", "CL_0009007", "UBERON_8410068"
        ]
      }
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
