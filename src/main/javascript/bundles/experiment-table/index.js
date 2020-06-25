import React from 'react'
import ReactDOM from 'react-dom'

import { TableManager, TableManagerRouter } from '@ebi-gene-expression-group/atlas-experiment-table'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'
import { loadExperimentDownloadModule } from '@ebi-gene-expression-group/atlas-experiment-table'

const TableManagerWithFetchLoader = withFetchLoader(TableManager)
const TableManagerRouterWithFetchLoader = withFetchLoader(TableManagerRouter)

const render = (options, target) => {
  ReactDOM.render(
    <TableManagerWithFetchLoader
      {...options}
      renameDataKeys={{experiments: `dataRows`}}/>,
    document.getElementById(target))
}

const renderRouter = (options, target) => {
  ReactDOM.render(
    <TableManagerRouterWithFetchLoader
      {...options}
      renameDataKeys={{experiments: `dataRows`}}/>,
    document.getElementById(target))
}

export { render, renderRouter, loadExperimentDownloadModule }
