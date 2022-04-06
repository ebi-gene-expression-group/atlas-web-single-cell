import React from 'react'
import ReactDOM from 'react-dom'

import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'
import AtlasInformationBanner from '@ebi-gene-expression-group/atlas-information-banner'

const AtlasInformationBannerWithFetchLoader = withFetchLoader(AtlasInformationBanner)

const render = (options, target) => {
    ReactDOM.render(
      <AtlasInformationBannerWithFetchLoader
        host={`https://ebi-gene-expression-group.github.io/`}
        resource={`scxa-motd.md`}
        errorPayloadProvider={ () => {} }
        loadingPayloadProvider={ () => {} }
        fulfilledPayloadProvider={ data => ({ motd: data }) }
        raw={true}
      />,
      document.getElementById(target))
}

export { render }
