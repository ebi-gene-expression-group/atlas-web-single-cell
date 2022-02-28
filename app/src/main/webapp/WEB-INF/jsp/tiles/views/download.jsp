<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="row">
    <div class="12 columns">
        <h2>Download</h2>

        <h3>Via FTP</h3>
        <p>
            You can download data for every dataset individually in Expression Atlas through our
            <a href="http://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/experiments/" target="_blank">FTP site</a>.
        </p>

        <h3>From experiment pages</h3>
        <p>
            You can download single experiment files from the <i>Downloads</i> tab in any experiment page. Below is
            a short summary about data that you can download.
        </p>

        <h4>Metadata Files</h4>
        <p>
            <img src="/gxa/sc/resources/images/download/download-tsv-file.png" alt="idf and sdrf file" align="bottom" class="icon"/>
            Experiment Metadata: SDRF and IDF files containing information about the experiment.
        </p>
        <p>
            <img src="/gxa/sc/resources/images/download/download-experiment-design.png" alt="experiment design file" align="bottom" class="icon"/>
            Experiment Design file: Information about assays within the experiment.
        </p>

        <h4>Results Files</h4>
        <p>
            <img src="/gxa/sc/resources/images/download/download-tsv-file.png" alt="clustering file" align="bottom" class="icon"/>
            Clustering file: Results of unsupervised louvain clustering at a range of resolution values.
        </p>
        <p>
            <img src="/gxa/sc/resources/images/download/download-tsv-file.png" alt="marker gene file" align="bottom" class="icon"/>
            Marker gene files: Results of marker gene detection, as calculated by Scanpy using the Wilcoxon rank-sum method.
        </p>
        <p>
            <img src="/gxa/sc/resources/images/download/download-tsv-file.png" alt="Normalised counts file" align="bottom" class="icon"/>
            Normalised counts files: Untransformed expression values, normalised to counts per million.
        </p>
        <p>
            <img src="/gxa/sc/resources/images/download/download-tsv-file.png" alt="raw counts file" align="bottom" class="icon"/>
            Raw counts files: Raw counts values before filtering and normalisation.
        </p>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function(event) {
        document.getElementById("local-nav-download").className += ' active';
    });
</script>
