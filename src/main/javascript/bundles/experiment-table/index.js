import React from 'react'
import ReactDOM from 'react-dom'

import ExperimentTable from 'atlas-experiment-table'
import { withFetchLoader } from 'atlas-react-fetch-loader'

const FetchLoadExperimentTable = withFetchLoader(ExperimentTable)

const render = (options, target) => {
  ReactDOM.render(<FetchLoadExperimentTable {...options}/>, document.getElementById(target))
}

export { render }
