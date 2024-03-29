import React from 'react'
import ReactDOM from 'react-dom'

import AtlasAutocomplete from 'expression-atlas-autocomplete'

const render = (options, target) => {
  ReactDOM.render(<AtlasAutocomplete {...options} />, document.getElementById(target))
}

export { render }
