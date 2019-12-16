import React from 'react'
import ReactDOM from 'react-dom'

import ExperimentTable from '@ebi-gene-expression-group/atlas-experiment-table'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const FetchLoadExperimentTable = withFetchLoader(ExperimentTable)

const render = (options, target) => {
  ReactDOM.render(<FetchLoadExperimentTable {...options}/>, document.getElementById(target))
}

export { render }
