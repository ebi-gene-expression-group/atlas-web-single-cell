import React from 'react'
import PropTypes from 'prop-types'

import _ from 'lodash'

import MarkerGeneHeatmap from '@ebi-gene-expression-group/scxa-marker-gene-heatmap/lib/MarkerGeneHeatmap'
import CellTypeWheel from '@ebi-gene-expression-group/scxa-cell-type-wheel'

class CellTypeWheelExperimentHeatmap extends React.Component {
  constructor(props) {
    super(props)
      this.state = {
        heatMapData: {}
      }
  }
    _fetchAndSetHeatmapData(
        {atlasUrl, experimentAccession}) {
        const resource = `json/experiments/${experimentAccession}/metadata/tsneplot`

        this._fetchAndSetState(
            resource, atlasUrl, `plotdata`, `metadataErrorMessage`, `loadingMetadata`) // this will work once backend code is merged in sc atlas. In meantime, it will not fetch metadata option as there is no endpoint currently in sc atlas
    }

  render() {
    const { cellTypeWheelData, cellTypeWheelSearchTerm, cellTypeHeatmapSearchTerm, cellTypeHeatmapData } = this.props
    const heatmapProps = {
      hasDynamicHeight: true
    }

    return (
      <div className={`row-expanded`}>
        <div className={`small-12 medium-6 columns`}>
          <CellTypeWheel
            data = {cellTypeWheelData}
            searchTerm = {cellTypeWheelSearchTerm}
          />
        </div>
        <div className={`small-12 medium-6 columns`}>
          <MarkerGeneHeatmap
            cellType={cellTypeHeatmapSearchTerm}
            data={cellTypeHeatmapData}
            xAxisCategories={_.chain(cellTypeHeatmapData).uniqBy(`x`).sortBy(`x`).map(`cellGroupValue`).value()}
            yAxisCategories={_.chain(cellTypeHeatmapData).uniqBy(`y`).sortBy(`y`).map(`geneName`).value()}
            hasDynamicHeight={_.chain(cellTypeHeatmapData).map(`geneName`).uniq().value().length > 5 ? heatmapProps.hasDynamicHeight : false}
            heatmapRowHeight={40}
            species={`Homo sapiens`}
            heatmapType={`celltypes`}
          />
        </div>
      </div>
    )
  }
}

CellTypeWheelExperimentHeatmap.propTypes = {
  cellTypeWheelSearchTerm: PropTypes.string.isRequired,
  cellTypeWheelData: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      id: PropTypes.string.isRequired,
      parent: PropTypes.string.isRequired,
      value: PropTypes.number
    })).isRequired,
  cellTypeHeatmapSearchTerm: PropTypes.string.isRequired,
  cellTypeHeatmapData: PropTypes.arrayOf(
    PropTypes.shape({
      x: PropTypes.number.isRequired,
      y: PropTypes.number.isRequired,
      geneName: PropTypes.string.isRequired,
      value: PropTypes.number.isRequired,
      cellGroupValue: PropTypes.string.isRequired,
      cellGroupValueWhereMarker: PropTypes.string.isRequired,
      pValue: PropTypes.number.isRequired
    })).isRequired
}

export default CellTypeWheelExperimentHeatmap
