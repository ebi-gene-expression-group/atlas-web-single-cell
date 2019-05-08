<%@ page contentType="text/html;charset=UTF-8" %>

<div id="experiments"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css" media="all" />

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/experimentTable.bundle.js"></script>

<script>
    document.addEventListener("DOMContentLoaded", function(event) {
      console.log(${data});
      experimentTable.render({
        host: '${pageContext.request.contextPath}/',
        resource: 'json/experiments',
        tableHeader: [
          {type: 'sort', title: 'Loaded date', width: 240, dataParam: 'lastUpdate'},
          {type: 'search', title: 'species', width: 260, dataParam: 'species'},
          {type: 'search', title: 'experiment description', width: 460, dataParam: 'experimentDescription',
              link: 'experimentAccession', resource: 'experiments', endpoint: 'Results'},
          {type: 'search', title: 'experiment factors', width: 260, dataParam: 'experimentalFactors'},
          {type: 'sort', title: 'Number of assays', width: 260, dataParam: 'numberOfAssays',
              link: 'experimentAccession', resource: 'experiments', endpoint: 'Experiment%20Design'}
        ],
        noResultsMessageFormatter: function(data) { return 'Your search yielded no results: ' + data.reason },
        enableDownload: true,
        aaData: ${data}
      }, 'experiments');
    });
</script>
