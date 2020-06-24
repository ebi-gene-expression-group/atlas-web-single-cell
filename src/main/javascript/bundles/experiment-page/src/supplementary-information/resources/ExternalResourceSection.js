import React, {Component} from 'react'
import PropTypes from 'prop-types'
import {connect} from 'react-refetch'
import {uniq} from 'lodash'
import URI from 'urijs'
import iconAe from "./icons/ae-logo-64.png";
import iconGseaReactome from "./icons/gsea_reactome-icon.png";
import iconGseaInterpro from "./icons/gsea_interpro-icon.png";
import iconGseaGo from "./icons/gsea_go-icon.png";
import iconMa from "./icons/ma-plot-icon.png";
import iconExperimentDesign from "./icons/experiment_design_icon.png";
import iconTsv from "./icons/download_blue_small.png";
import iconRData from "./icons/r-button.png";
import iconEga from "./icons/ega.png";
import iconEna from "./icons/ena.png";
import iconGeo from "./icons/geo.png";

const Icon = ({type}) => {
  const iconSrcMap = {
    'icon-ae': iconAe,
    'icon-gsea-reactome': iconGseaReactome,
    'icon-gsea-interpro': iconGseaInterpro,
    'icon-gsea-go': iconGseaGo,
    'icon-ma': iconMa,
    'icon-experiment-design': iconExperimentDesign,
    'icon-tsv': iconTsv,
    'icon-Rdata': iconRData,
    'icon-ega': iconEga,
    'icon-ena': iconEna,
    'icon-geo': iconGeo
  }

  return (
    <img style={{marginRight: `0.5rem`, height: `32px`}} src={iconSrcMap[type]} />
  )
}

Icon.propTypes = {
  type: PropTypes.string.isRequired
}

const ResourcesSection = ({values, atlasUrl}) => {
  const subsections = uniq(values.map((value)=> (
    value.group
  )))

  return (
    <div className="row column expanded margin-top-large">
      <ul style={{listStyle: `none`}}>
        {
          subsections.filter(el=>el).length < 2
            ? values.map((value, ix, self) => (
              <li key={ix}>
                <a href={URI(value.url, atlasUrl)}>
                  <p>
                    <Icon type={value.type} />
                    {value.description}
                  </p>
                </a>
              </li>
            ))
            : subsections.map((subsectionName, ix) => (
              <li key={ix}>
                <ul style={{listStyle: `none`}} className="margin-left-none margin-bottom-medium">
                  <i>{
                    subsectionName}
                  </i>
                  {
                    values.filter((value) => (
                      subsectionName === value.group
                    ))
                      .map((value, jx, self) => (
                        <li key={jx} className="margin-left-large">
                          <a href={URI(value.url, atlasUrl)}>
                            <div>
                              <p>
                                <Icon type={value.type} />
                                {value.description}
                              </p>
                            </div>
                          </a>
                        </li>
                      ))
                  }
                </ul>
              </li>
            ))
        }
      </ul>
    </div>
  )
}


class ResourcesTab extends Component {
  render() {
    const {resourcesFetch, atlasUrl} = this.props

    if (resourcesFetch.pending) {
      return (
        <div className={`row column expanded margin-top-large`}>
          <img src={URI(`resources/images/loading.gif`, atlasUrl)} />
        </div>
      )
    } else if (resourcesFetch.rejected) {
      return (
        <div className={`row column expanded margin-top-large`}>
          <p>Error: {resourcesFetch.reason}</p>
        </div>
      )
    } else if (resourcesFetch.fulfilled) {
      return (
          <ResourcesSection
            values={resourcesFetch.value}
            atlasUrl={atlasUrl} />
      )
    }
  }
}

export default connect(props => ({
  resourcesFetch: URI(props.url, props.atlasUrl).toString()
}))(ResourcesTab)

