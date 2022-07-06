<%--@elvariable id="resourcesVersion" type="String"--%>
<%--@elvariable id="geneId" type="String"--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<ul class="tabs" data-tabs id="search-tabs">
    <li class="tabs-title is-active"><a href="#search-atlas" aria-selected="true">Search</a></li>
</ul>

<div class="tabs-content" data-tabs-content="search-tabs">
    <div class="tabs-panel is-active " id="search-atlas" style="background-color: #e6e6e6;">
        <div id="search-form"></div>
    </div>
</div>

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/geneSearchForm.bundle.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function (event) {
        geneSearchForm.render({
            host: '${pageContext.request.contextPath}/',
            resource: 'json/suggestions/species',
            wrapperClassName: 'row expanded',
            actionEndpoint: 'search',

            autocompleteClassName: 'small-12 medium-8 columns',
            suggesterEndpoint: 'json/suggestions/gene_ids',

            enableSpeciesSelect: true,
            speciesSelectClassName: 'small-12 medium-4 columns',

            autocompleteLabel: '',
            searchExamples: [
                {
                    text: 'CFTR (gene symbol)',
                    url: '${pageContext.request.contextPath}/search?symbol=CFTR'
                },
                {
                    text: 'ENSG00000115904 (Ensembl ID)',
                    url: '${pageContext.request.contextPath}/search?ensgene=ENSG00000115904'
                },
                {
                    text: '657 (Entrez ID)',
                    url: '${pageContext.request.contextPath}/search?entrezgene=657'
                },
                {
                    text: 'MGI:98354 (MGI ID)',
                    url: '${pageContext.request.contextPath}/search?mgi_id=MGI:98354'
                },
                {
                    text: 'FBgn0004647 (FlyBase ID)',
                    url: '${pageContext.request.contextPath}/search?flybase_gene_id=FBgn0004647'
                },
                {
                    text: 'keratinocyte (cell type)',
                    url: '${pageContext.request.contextPath}/search/metadata/keratinocyte'
                },
                {
                    text: 'liver (organ/organism part)',
                    url: '${pageContext.request.contextPath}/search/metadata/liver'
                },
                {
                    text: 'lung cancer (disease/condition)',
                    url: '${pageContext.request.contextPath}/search/metadata/lung cancer'
                }
            ]
        }, 'search-form')
    });
</script>
