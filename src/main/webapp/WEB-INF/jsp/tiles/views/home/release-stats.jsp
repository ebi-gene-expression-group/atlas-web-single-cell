<%--@elvariable id="info" type="ImmutableMap<String, String>"--%>
<%--@elvariable id="numberOfSpecies" type="Number"--%>
<%--@elvariable id="numberOfStudies" type="Number"--%>
<%--@elvariable id="numberOfAssays" type="Number"--%>
<%--@elvariable id="ensembl" type="String"--%>
<%--@elvariable id="eg" type="String"--%>
<%--@elvariable id="wbps" type="String"--%>
<%--@elvariable id="efo" type="String"--%>
<%--@elvariable id="efoURL" type="String"--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="small-12 medium-6 columns text-left">
  <h4>
    <small>
      Search across <strong><fmt:formatNumber value="${numberOfSpecies}"/>&nbsp;species</strong>,
      <strong><fmt:formatNumber value="${numberOfStudies}"/>&nbsp;studies</strong>,
      <strong><fmt:formatNumber value="${numberOfAssays}"/>&nbsp;assays</strong>
    </small>
  </h4>
</div>
<div class="small-12 medium-6 columns hide-for-small-only text-right">
  <h4>
    <small>
      Ensembl&nbsp;${info.get(ensembl)},
      Ensembl&nbsp;Genomes&nbsp;${info.get(eg)},
      WormBase&nbsp;ParaSite&nbsp;${info.get(wbps)},
      <a href="${info.get(efoURL)}">EFO&nbsp;${info.get(efo)}</a>
    </small>
  </h4>
</div>
