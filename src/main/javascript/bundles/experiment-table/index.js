import React from 'react'
import ReactDOM from 'react-dom'

import { TableManagerSpa } from '@ebi-gene-expression-group/atlas-experiment-table'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const FetchLoadExperimentTable = withFetchLoader(TableManagerSpa)

const render = (options, target) => {
  ReactDOM.render(
    <FetchLoadExperimentTable
      {...options}
      resource={`json/experiments`}
      renameDataKeys={{experiments: `dataRows`}}/>,
    document.getElementById(target))
}

export { render }
