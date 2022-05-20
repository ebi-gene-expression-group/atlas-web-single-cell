import React from 'react'
import ReactDOM from 'react-dom'

import CellTypeWheelMultiExperimentHeatmap from './src/CellTypeWheelMultiExperimentHeatmap'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const CellTypeWheelMultiExperimentHeatmapFetchLoader = withFetchLoader(CellTypeWheelMultiExperimentHeatmap)

const render = (options, target) => {
    ReactDOM.render(
        <CellTypeWheelMultiExperimentHeatmapFetchLoader
            {...options}
            fulfilledPayloadProvider={ data => ({
                    cellTypeWheelData: data
                })}
        />,
        document.getElementById(target))
}

export { render }