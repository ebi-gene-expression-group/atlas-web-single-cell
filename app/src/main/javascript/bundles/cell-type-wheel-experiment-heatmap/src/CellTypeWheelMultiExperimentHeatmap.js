import React from 'react'
import PropTypes from 'prop-types'

import CellTypeWheelExperimentHeatmap from '@ebi-gene-expression-group/scxa-cell-type-wheel-heatmap'

class CellTypeWheelMultiExperimentHeatmap extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    const { cellTypeWheelData, cellTypeWheelSearchTerm } = this.props

    return (
      <div className={`row-expanded`}>
          <CellTypeWheelExperimentHeatmap
              cellTypeWheelSearchTerm={cellTypeWheelSearchTerm}
              cellTypeWheelData={cellTypeWheelData}
          />
      </div>
    )
  }
}

CellTypeWheelMultiExperimentHeatmap.propTypes = {
  cellTypeWheelSearchTerm: PropTypes.string.isRequired,
  cellTypeWheelData: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      id: PropTypes.string.isRequired,
      parent: PropTypes.string.isRequired,
      value: PropTypes.number
    })).isRequired
}

export default CellTypeWheelMultiExperimentHeatmap
