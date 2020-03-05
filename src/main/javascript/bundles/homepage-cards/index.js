import React from 'react'
import ReactDOM from 'react-dom'

import ResponsiveCardsRow, { ExtendableCard } from '@ebi-gene-expression-group/atlas-homepage-cards'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const FetchLoadResponsiveCardsRow = withFetchLoader(ResponsiveCardsRow)

const render = (options, target) => {
  ReactDOM.render(
    <FetchLoadResponsiveCardsRow
      CardClass={ExtendableCard}
      {...options}
    />,
    document.getElementById(target))
}

export { render }
