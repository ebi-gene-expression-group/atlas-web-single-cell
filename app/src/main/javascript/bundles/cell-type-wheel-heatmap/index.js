import React from 'react'
import ReactDOM from 'react-dom'

import CellTypeWheelMultiExperimentHeatmap from '@ebi-gene-expression-group/scxa-cell-type-wheel-heatmap'

const render = (options, target) => {
  ReactDOM.render(
    <CellTypeWheelMultiExperimentHeatmap {...options}
    />,
    document.getElementById(target))
}

export { render }
