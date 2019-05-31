<%@ page contentType="text/html;charset=UTF-8" %>
<div id="species-summary-panel"></div>
<div class="row expanded column text-center margin-top-medium">
  <a class="button primary" href="${pageContext.request.contextPath}/experiments">Show experiments of all species</a>
</div>
<link rel="stylesheet" type="text/css" charset="UTF-8" href="https://cdnjs.cloudflare.com/ajax/libs/slick-carousel/1.6.0/slick.min.css" />
<link rel="stylesheet" type="text/css" charset="UTF-8" href="https://cdnjs.cloudflare.com/ajax/libs/slick-carousel/1.6.0/slick-theme.min.css" />

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/homepageSpeciesSummaryPanel.bundle.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/atlas-slide.css" type="text/css" media="all">

<!-- Set to http://localhost:8080/gxa/ or http://localhost:8080/gxa_sc/ -- Remember the trailing slash! -->
<script>
  document.addEventListener("DOMContentLoaded", function(event) {
    var slideSettings = {
      dots: true,
      infinite: true,
      speed: 500,
      slidesToShow: 6,
      slidesToScroll: 6,
      autoplay: true,
      autoplaySpeed: 2000,
      adaptiveHeight: true,
      responsive: [
        {
          breakpoint: 1024,
          settings: {
            slidesToShow: 3,
            slidesToScroll: 3,
            infinite: true,
            dots: true
          }
        },
        {
          breakpoint: 600,
          settings: {
            slidesToShow: 2,
            slidesToScroll: 2,
            dots: true
          }
        },
        {
          breakpoint: 480,
          settings: {
            slidesToShow: 1,
            slidesToScroll: 1,
            dots: true
          }
        }
      ]
    };
    homepageSpeciesSummaryPanel.render({
      host: 'https://www.ebi.ac.uk/gxa/sc/',
      resource: 'json/species-summary?limit=100',
        carouselCardsRowProps: {
        className: 'row expanded small-up-2 medium-up-3 large-up-6',
        cardContainerClassName: 'column',
        slideSettings: slideSettings
      },
      onComponentDidMount: function() {
        $('#species-summary-panel').foundation();
        $('#species-summary-panel').foundationExtendEBI();
      }
    },
    'species-summary-panel');
  });
</script>
