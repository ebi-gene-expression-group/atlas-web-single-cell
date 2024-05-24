import React from 'react'
import PropTypes from "prop-types"
import {
  IconDiv,
  MarkerDiv,
  TitleDiv,
  VariableDiv,
  CountDiv,
  CardContainerDiv
} from './ExperimentsHeaderDiv'


const ExperimentTableHeaderBasic = () =>
  ({
    'titles': [`Species`, `Marker genes`, `Title`, `Experimental variables`, `Number of assays`],
    'styles': [IconDiv, MarkerDiv, TitleDiv, VariableDiv, CountDiv],
    'attributes': [`species`, `markerGenes`, `experimentDescription`, null, `numberOfAssays`]
  })


class ExperimentsHeader extends  React.Component {
  constructor(props) {
    super(props)

    this.state = {
      sortTitle: `markerGenes`,
      ascending: false
    }

    this.onClick = this.onClick.bind(this)
  }

  onClick(attribute){
    this.props.onClick(attribute)
    this.setState({
      sortTitle: attribute,
      ascending: !this.state.ascending
    })
  }

  render() {
    const tableTitles = ExperimentTableHeaderBasic().titles
    const tableTitleDivs = ExperimentTableHeaderBasic().styles
    const jsonAttributes = ExperimentTableHeaderBasic().attributes

    return(
      <CardContainerDiv>
        {
          tableTitles.map((title, index) => {
            const TitleDiv = tableTitleDivs[index]
            const attribute = jsonAttributes[index]
            return attribute ?
              attribute === this.state.sortTitle ?
                <TitleDiv key={title} style={{opacity: 1}}><span id={`selected`} onClick={() => this.onClick(attribute)}>{`${title} `}
                  {this.state.ascending ? <i className="icon icon-common icon-sort-up"/> : <i className="icon icon-common icon-sort-down"/>}</span></TitleDiv>
                : <TitleDiv key={title}><span id={`title`} onClick={() => this.onClick(attribute, this.state.ascending)}>{title} <i className={`icon icon-common icon-sort`}/></span></TitleDiv>
              : <TitleDiv key={title}><span id={`title`}>{title}</span></TitleDiv>
          })
        }
      </CardContainerDiv>
    )
  }
}

ExperimentsHeader.propTypes = {
  onClick: PropTypes.func.isRequired
}

export default ExperimentsHeader
