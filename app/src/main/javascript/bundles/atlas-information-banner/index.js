import React from 'react'
import ReactDOM from 'react-dom'

import AtlasInformationBanner from '@ebi-gene-expression-group/atlas-information-banner'

const render = (options, target) => {
    ReactDOM.render(<AtlasInformationBanner {...options} />, document.getElementById(target))
}

export { render }