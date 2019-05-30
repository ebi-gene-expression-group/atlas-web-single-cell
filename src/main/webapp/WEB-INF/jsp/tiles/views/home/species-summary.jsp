<%@ page contentType="text/html;charset=UTF-8" %>
<div id="species-summary-panel"></div>
<div class="row expanded column text-center margin-top-medium">
  <a class="button primary" href="${pageContext.request.contextPath}/experiments">Show experiments of all species</a>
</div>
<link rel="stylesheet" type="text/css" charset="UTF-8" href="https://cdnjs.cloudflare.com/ajax/libs/slick-carousel/1.6.0/slick.min.css" />
<link rel="stylesheet" type="text/css" charset="UTF-8" href="https://cdnjs.cloudflare.com/ajax/libs/slick-carousel/1.6.0/slick-theme.min.css" />
<style>
  .slick-slide img {
    margin: auto;
  }
  .slick-slider {
    margin: 30px auto 50px;
  }
  .slick-prev:before {
    color: #3497c5;
  }

  .slick-next:before{
    color: #3497c5;
  }
  .slick-prev:hover {
    color: #2f5767;
  }
  .slick-next:hover{
    color: #2f5767;
  }
</style>
<script defer src="${pageContext.request.contextPath}/resources/js-bundles/homepageSpeciesSummaryPanel.bundle.js"></script>

<!-- Set to http://localhost:8080/gxa/ or http://localhost:8080/gxa_sc/ -- Remember the trailing slash! -->
<script>
  document.addEventListener("DOMContentLoaded", function(event) {
    var slideSettings = {
      dots: true,
      infinite: true,
      speed: 500,
      slidesToShow: 6,
      slidesToScroll: 1,
      autoplay: true,
      autoplaySpeed: 2000,
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
            slidesToScroll: 2
          }
        },
        {
          breakpoint: 480,
          settings: {
            slidesToShow: 1,
            slidesToScroll: 1
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
