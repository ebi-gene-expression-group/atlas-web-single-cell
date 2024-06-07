import React from 'react'
import PropTypes from 'prop-types'
import AnalysisMethodsTable from './supplementary-information/AnalysisMethodsTable'
import Resources from './supplementary-information/resources/ExternalResourceSection'

const sectionTypeComponent = {
  'static-table' : AnalysisMethodsTable,
  'resources' : Resources
}

const SectionContent = ({type, props, atlasUrl}) => {
  const Section = sectionTypeComponent[type]

  return (Section && <Section {...{...props, atlasUrl}} />)
}

const SupplementaryInformationRoute = (props) => {
  const annDataText =
      'This methods summary is based on information provided by the authors or described in the associated ' +
      'publication, the data have not been re-analysed by Single Cell Expression Atlas'

  const sections = props.sections.map((section) =>
    {
      const isAnnDataTextNeeded =
        props.experimentAccession.startsWith('E-ANND') && section.name === `Analysis Methods`

      return (
        <div key={section.name}>
          <h3>{section.name}</h3>
          { isAnnDataTextNeeded && <p><i>{annDataText}</i></p> }
          <SectionContent type={section.type} props={section.props} atlasUrl={props.atlasUrl}/>
        </div>
      )
    }
  )

  return (
    <div className={`row expanded margin-top-large`}>
      <div className={`small-12 columns`}>
        {sections}
      </div>
    </div>
  )
}

SupplementaryInformationRoute.propTypes = {
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  history: PropTypes.object.isRequired,
  atlasUrl: PropTypes.string.isRequired,
  resourcesUrl: PropTypes.string,
  experimentAccession: PropTypes.string.isRequired,
  sections: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    props: PropTypes.object.isRequired
  }))
}

export default SupplementaryInformationRoute