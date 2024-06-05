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
  gut: `UBERON_0000160`,
  ovary: ``,
  reproduction: ``,
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
    let requestOntologyIds = [];

    switch (species) {
      case 'kidney':
        switch (view) {
          case 'kidney':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'nephron':
            requestOntologyIds = ['UBERON_0001285'];
            break;
          case 'glomerulus':
            requestOntologyIds = ['UBERON_0000074'];
            break;
          case 'duct':
            requestOntologyIds = ['UBERON_0001232'];
            break;
        }
        break;

      case 'liver':
        switch (view) {
          case 'liver':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'lobule':
            requestOntologyIds = [
              'CL_0000182', 'CL_0000632', 'UBERON_0001282', 'CL_1000488', 'UBERON_0001193', 'UBERON_0006841',
              'UBERON_0001639', 'EFO_0010704', 'UBERON_0001281', 'CL_1000398', 'EFO_0010705', 'EFO_0010706', 'CL_0000091'
            ];
            break;
        }
        break;

      case 'lung':
        switch (view) {
          case 'lung':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'airway':
            requestOntologyIds = [
              'CL_0002633', 'CL_1001567', 'CL_1000271', 'CL_0000158', 'EFO_0010666', 'CL_0002370', 'UBERON_0003504',
              'CL_0002598'
            ];
            break;
          case 'alveoli_fin':
            requestOntologyIds = ['UBERON_0002299', 'UBERON_0002188', 'UBERON_0016405'];
            break;
          case 'alveoli_section':
            requestOntologyIds = [
              'CL_0002062', 'CL_0002063', 'UBERON_0003456', 'EFO_0010667', 'EFO_0010668', 'EFO_0010669'
            ];
            break;
        }
        break;

      case 'pancreas':
        switch (view) {
          case 'pancreas':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'acinus':
            requestOntologyIds = [
              'CL_0000173', 'CL_0000171', 'CL_0000169', 'CL_0002275', 'CL_0005019', 'CL_0002410', 'CL_0000622'
            ];
            break;
        }
        break;

      case 'placenta':
        switch (view) {
          case 'placenta':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'cells':
            requestOntologyIds = [
              'CL_2000002', 'EFO_0010710', 'CL_3000001', 'UBERON_0000371', 'UBERON_0000319', 'UBERON_0000426',
              'EFO_0010712', 'EFO_0010711', 'CL_0002601', 'EFO_0010708'
            ];
            break;
        }
        break;

      case 'gut':
        switch (view) {
          case 'gut':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'colon':
            requestOntologyIds = [
              'UBERON_8410051', 'UBERON_8410015', 'UBERON_8410048', 'UBERON_8410057',
              'UBERON_0007178', 'UBERON_8410000', 'UBERON_8410061', 'UBERON_8410058', 'UBERON_8410059',
              'CL_0009043', 'CL_0009042', 'CL_0009041', 'CL_1000347', 'CL_0009039', 'CL_0009011',
              'CL_0009009', 'CL_0009025', 'CL_0009038', 'CL_0009040', 'UBERON_8410060'
            ];
            break;
          case 'intestine':
            requestOntologyIds = [
              'CL_1000275', 'CL_1000334', 'UBERON_8410002', 'UBERON_0018410', 'UBERON_8410004',
              'UBERON_8410001', 'UBERON_8410063', 'UBERON_8410064', 'UBERON_0001210', 'UBERON_0012401',
              'UBERON_0012402', 'CL_0009012', 'CL_1000495', 'CL_1000343', 'CL_1000353', 'CL_0009080',
              'CL_0009017', 'CL_0009006', 'CL_0009024', 'CL_0009022', 'CL_1000411', 'CL_0009015', 'CL_0009007', 'UBERON_8410068'
            ];
            break;
        }
        break;

      case 'ovary':
        switch (view) {
          case 'ovary':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'ovariole':
            requestOntologyIds = [
              'FBbt_00048555', 'FBbt_00048554', 'FBbt_00048556', 'FBbt_00004906', 'FBbt_00004905',
              'FBbt_00000046', 'FBbt_00000043', 'FBbt_00000045', 'FBbt_00000047', 'FBbt_00004878',
              'FBbt_00007031', 'FBbt_00004866', 'FBbt_00004886', 'FBbt_00005406', 'FBbt_00004903'
            ];
            break;
          case 'germarium':
            requestOntologyIds = [
              'FBbt_00048532', 'FBbt_00004886', 'FBbt_00048534', 'FBbt_00048531', 'FBbt_00004877',
              'FBbt_00004903', 'FBbt_00048537', 'FBbt_00047835', 'FBbt_00048536', 'FBbt_00048535',
              'FBbt_00004873', 'FBbt_00004868', 'FBbt_00047029', 'FBbt_00004904', 'FBbt_00048288',
              'FBbt_00048533'
            ];
            break;
        }
        break;

      case 'reproduction':
        switch (view) {
          case 'reproduction':
            requestOntologyIds = [supportedOrgans[species]];
            break;
          case 'testis':
            requestOntologyIds = [
              'FBbt_00057009', 'FBbt_00057010', 'FBbt_00004942', 'FBbt_00004941', 'FBbt_00059174',
              'FBbt_00059175', 'FBbt_00005286', 'FBbt_00004929', 'FBbt_00004933', 'FBbt_00059172',
              'FBbt_00004935', 'FBbt_00004931', 'FBbt_00004934', 'FBbt_00004930', 'FBbt_00004928',
              'FBbt_00005287', 'FBbt_00004938', 'FBbt_00007138', 'FBbt_00004931', 'FBbt_00007141',
              'FBbt_00005259', 'FBbt_00004954'
            ];
            break;
        }
        break;

      default:
        break;
    }

    return requestOntologyIds;
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
