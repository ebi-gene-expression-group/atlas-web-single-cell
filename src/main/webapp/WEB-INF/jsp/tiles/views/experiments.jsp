<%@ page contentType="text/html;charset=UTF-8" %>

<div id="experiments"></div>
<div id="popup-placeholder"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css"
      media="all"/>

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/experimentTable.bundle.js"></script>

<script>
    document.addEventListener('DOMContentLoaded', function (event) {
        experimentTable.renderRouter(
            {
                tableHeaders: [
                    {
                        label: 'Load date',
                        dataKey: 'loadDate',
                        sortable: true,
                        width: 0.5
                    },
                    {
                        label: 'Species',
                        dataKey: 'species',
                        searchable: true,
                        sortable: true
                    },
                    {
                        label: 'Title',
                        dataKey: 'experimentDescription',
                        searchable: true,
                        sortable: true,
                        linkTo: function (dataRow) {
                            return 'experiments/' + dataRow.experimentAccession + '/results';
                        },
                        width: 2
                    },
                    {
                        label: 'Experimental factors',
                        dataKey: 'experimentalFactors',
                        searchable: true
                    },
                    {
                        label: 'Number of cells',
                        dataKey: 'numberOfAssays',
                        sortable: true,
                        linkTo: function (dataRow) {
                            return 'experiments/' + dataRow.experimentAccession + '/experiment-design';
                        },
                        width: 0.5
                    }
                ],
                dropdownFilters: [
                    {
                        label: 'Kingdom',
                        dataKey: 'kingdom'
                    },
                    {
                        label: 'Experiment collection',
                        dataKey: 'experimentProjects'
                    },
                    {
                        label: 'Technology type',
                        dataKey: 'technologyType'
                    }
                ],
                rowSelectionColumn: {
                    label: 'Download',
                    dataKey: 'experimentAccession',
                    tableHeaderCellOnClick:
                        experimentTable.loadExperimentDownloadModule(
                            'https://www.ebi.ac.uk/gxa/sc/',
                            [
                                {
                                    description: 'Raw quantification files',
                                    id: 'quantification-raw'
                                },
                                {
                                    description: 'Normalised counts files',
                                    id: 'normalised'
                                },
                                {
                                    description: 'Experiment design file',
                                    id: 'experiment-design'
                                }
                            ],
                            'popup-placeholder'),
                    tooltipContent:
                        '<ul>' +
                        '<li>Raw filtered count matrix after quantification</li>' +
                        '<li>Normalised filtered count matrix after quantification</li>' +
                        '<li>Experiment design file with experimental metadata</li>' +
                        '</ul>',
                    width: 0.5
                },
                sortColumnIndex: 0,
                ascendingOrder: false,
                host: '${pageContext.request.contextPath}/',
                resource: 'json/experiments',
                basename: '${pageContext.request.contextPath}'
            },
            'experiments');
    });
</script>
