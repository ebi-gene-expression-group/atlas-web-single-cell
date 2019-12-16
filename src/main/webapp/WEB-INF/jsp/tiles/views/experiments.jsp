<%@ page contentType="text/html;charset=UTF-8" %>

<div id="experiments"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css" media="all" />

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/experimentTable.bundle.js"></script>

<script>
    document.addEventListener('DOMContentLoaded', function(event) {
      experimentTable.render({
        host: '${pageContext.request.contextPath}/',
        resource: 'json/experiments',
        downloadTooltip: '<ul>' +
          '<li>Raw filtered count matrix after quantification</li>' +
          '<li>Normalised filtered count matrix after quantification</li>' +
          '<li>Experiment design file with experimental metadata</li>' +
        '</ul>',
        tableHeader: [
          {type: 'sort', title: 'Loaded date', width: 240, dataParam: 'lastUpdate'},
          {type: 'search', title: 'species', width: 260, dataParam: 'species'},
          {type: 'search', title: 'experiment description', width: 460, dataParam: 'experimentDescription',
              link: 'experimentAccession', resource: 'experiments', endpoint: 'results'},
          {type: 'search', title: 'experiment factors', width: 260, dataParam: 'experimentalFactors'},
          {type: 'sort', title: 'Number of assays', width: 260, dataParam: 'numberOfAssays',
              link: 'experimentAccession', resource: 'experiments', endpoint: 'experiment-design'}
        ],
          tableFilters : [
              {label: `Kingdom`, dataParam: `kingdom`},
              {label: `Experiment Project`, dataParam: `experimentProjects`},
              {label: `Technology Type`, dataParam: `technologyType`}
          ],
        species: '${species}',
        enableDownload: true
      }, 'experiments');
    });
</script>
