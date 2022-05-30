<%--@elvariable id="cellTypeWheelSearchTerm" type="String"--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<div id="cellTypeWheelHeatmap"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css"
      media="all"/>

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/cellTypeWheelHeatmap.bundle.js"></script>

<script>
    document.addEventListener('DOMContentLoaded', function (event) {
        cellTypeWheelHeatmap.render(
            {
                host: '${pageContext.request.contextPath}/',
                resource: 'json/cell-type-wheel/${cellTypeWheelSearchTerm}',
                cellTypeWheelSearchTerm: '${cellTypeWheelSearchTerm}'
            },
            'cellTypeWheelHeatmap');
    });
</script>
