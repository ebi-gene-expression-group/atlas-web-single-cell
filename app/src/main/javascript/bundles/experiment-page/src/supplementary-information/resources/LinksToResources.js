import React from 'react'
import PropTypes from 'prop-types'
import URI from 'urijs'

import iconAe from './icons/ae-logo-64.png'
import iconGseaReactome from './icons/gsea_reactome-icon.png'
import iconGseaInterpro from './icons/gsea_interpro-icon.png'
import iconGseaGo from './icons/gsea_go-icon.png'
import iconMa from './icons/ma-plot-icon.png'
import iconExperimentDesign from './icons/experiment_design_icon.png'
import iconTsv from './icons/download_blue_small.png'
import iconRData from './icons/r-button.png'

const LinksToResources = ({data, atlasUrl}) => {
  const links =  data.map((service, index) =>
    <div  key={index} style={{paddingBottom: `10px`}}>
      <span>
        <Icon type={service.type} />
        {
          service.isDownload && atlasUrl ?
            <a href={URI(service.url, atlasUrl)}>{service.description}</a> :
            <a href={service.url}>{service.description}</a>
        }
      </span>
    </div>
  )

  return (
    <div>
      {links}
    </div>
  )
}

const Icon = ({type}) => {
  const iconSrcMap = {
    'icon-ae': iconAe,
    'icon-gsea-reactome': iconGseaReactome,
    'icon-gsea-interpro': iconGseaInterpro,
    'icon-gsea-go': iconGseaGo,
    'icon-ma': iconMa,
    'icon-experiment-design': iconExperimentDesign,
    'icon-tsv': iconTsv,
    'icon-Rdata': iconRData
  }

  return (
    <img style={{paddingRight: `5px`}} src={iconSrcMap[type]} />
  )
}

Icon.propTypes = {
  type: PropTypes.string.isRequired
}

LinksToResources.propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    group: PropTypes.string,
    type: PropTypes.string,
    description: PropTypes.string,
    url: PropTypes.string.isRequired,
    isDownload: PropTypes.bool
  })).isRequired,
  atlasUrl: PropTypes.string
}

export default LinksToResources

