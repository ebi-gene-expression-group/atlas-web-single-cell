import React from 'react'
import ReactDOM from 'react-dom'

import { CellTypeWheelExperimentHeatmap } from '@ebi-gene-expression-group/scxa-cell-type-wheel-heatmap'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const CellTypeWheelExperimentHeatmapFetchLoader = withFetchLoader(CellTypeWheelExperimentHeatmap)

const render = (options, target) => {
    ReactDOM.render(
        <CellTypeWheelExperimentHeatmapFetchLoader
            {...options} />,
        document.getElementById(target))
}

export { render }