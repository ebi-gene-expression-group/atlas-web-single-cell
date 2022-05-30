<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="row">
    <div class="12 columns">
        <h2>Download</h2>

        <h3>Via FTP</h3>
        <p>
            You can download data for every dataset individually in Expression Atlas through our
            <a href="http://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/sc_experiments/" target="_blank">FTP site</a>.
        </p>

        <h3>From experiment pages</h3>
        <p>
            You can download single experiment files from the <i>Downloads</i> tab in any experiment page. Below is
            a short summary about data that you can download.
        </p>

        <h4>Metadata files</h4>
        <p>
            <i class="icon icon-common icon-download"/>
            Experiment metadata: SDRF and IDF files containing information about the experiment.
        </p>
        <p>
            <i class="icon icon-fileformats icon-TSV"/>
            Experiment design file: Information about assays within the experiment.
        </p>

        <h4>Results Files</h4>
        <p>
            <i class="icon icon-fileformats icon-TSV"/>
            Clustering file: Results of unsupervised louvain clustering at a range of resolution values.
        </p>
        <p>
            <i class="icon icon-fileformats icon-TSV"/>
            Marker gene files: Results of marker gene detection, as calculated by Scanpy using the Wilcoxon rank-sum method.
        </p>
        <p>
            <i class="icon icon-common icon-download"/>
            Normalised counts files: Untransformed expression values, normalised to counts per million.
        </p>
        <p>
            <i class="icon icon-common icon-download"/>
            Raw counts files: Raw counts values before filtering and normalisation.
        </p>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function(event) {
        document.getElementById("local-nav-download").className += ' active';
    });
</script>
