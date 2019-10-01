<%--@elvariable id="info" type="ImmutableMap<String, String>"--%>
<%--@elvariable id="numberOfSpecies" type="Number"--%>
<%--@elvariable id="numberOfStudies" type="Number"--%>
<%--@elvariable id="numberOfCells" type="Number"--%>
<%--@elvariable id="ensembl" type="String"--%>
<%--@elvariable id="genomes" type="String"--%>
<%--@elvariable id="paraSite" type="String"--%>
<%--@elvariable id="efo" type="String"--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="small-12 medium-6 columns text-left">
  <h4>
    <small>
      Search across <strong><fmt:formatNumber value="${numberOfSpecies}"/>&nbsp;species</strong>,
      <strong><fmt:formatNumber value="${numberOfStudies}"/>&nbsp;studies</strong>
      <c:if test="${numberOfCells != 0}">,
      <strong><fmt:formatNumber value="${numberOfCells}"/>&nbsp;cells</strong>
      </c:if>
    </small>
  </h4>
</div>
<div class="small-12 medium-6 columns hide-for-small-only text-right">
  <h4>
    <small>
      Ensembl&nbsp;${info.get(ensembl)},
      Ensembl&nbsp;Genomes&nbsp;${info.get(genomes)},
      WormBase&nbsp;ParaSite&nbsp;${info.get(paraSite)},
      EFO&nbsp;${info.get(efo)}
    </small>
  </h4>
</div>
