var homepageSpeciesSummaryPanel=(window.webpackJsonp_name_=window.webpackJsonp_name_||[]).push([[9],{118:function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),Object.defineProperty(t,"default",{enumerable:!0,get:function(){return r.default}});var o,r=(o=n(268))&&o.__esModule?o:{default:o}},267:function(e,t,n){"use strict";n.r(t),n.d(t,"render",function(){return l});var o=n(1),r=n.n(o),a=n(4),u=n.n(a),c=n(118),i=n.n(c),s=n(21),f=Object(s.withFetchLoader)(i.a),l=function(e,t){u.a.render(r.a.createElement(f,e),document.getElementById(t))}},268:function(e,t,n){"use strict";function o(e){return(o="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":typeof e})(e)}Object.defineProperty(t,"__esModule",{value:!0}),t.default=void 0;var r=c(n(1)),a=c(n(0)),u=n(29);function c(e){return e&&e.__esModule?e:{default:e}}function i(e){return(i="function"==typeof Symbol&&"symbol"===o(Symbol.iterator)?function(e){return o(e)}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":o(e)})(e)}function s(){return(s=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var o in n)Object.prototype.hasOwnProperty.call(n,o)&&(e[o]=n[o])}return e}).apply(this,arguments)}function f(e,t){for(var n=0;n<t.length;n++){var o=t[n];o.enumerable=o.enumerable||!1,o.configurable=!0,"value"in o&&(o.writable=!0),Object.defineProperty(e,o.key,o)}}function l(e,t){return!t||"object"!==i(t)&&"function"!=typeof t?function(e){if(void 0===e)throw new ReferenceError("this hasn't been initialised - super() hasn't been called");return e}(e):t}function p(e){return(p=Object.setPrototypeOf?Object.getPrototypeOf:function(e){return e.__proto__||Object.getPrototypeOf(e)})(e)}function d(e,t){return(d=Object.setPrototypeOf||function(e,t){return e.__proto__=t,e})(e,t)}var y="species-summary-tabs",m=function(e){function t(){return function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,t),l(this,p(t).apply(this,arguments))}var n,o,a;return function(e,t){if("function"!=typeof t&&null!==t)throw new TypeError("Super expression must either be null or a function");e.prototype=Object.create(t&&t.prototype,{constructor:{value:e,writable:!0,configurable:!0}}),t&&d(e,t)}(t,r["default"].Component),n=t,(o=[{key:"render",value:function(){var e=this.props,t=e.speciesSummary,n=e.carouselCardsRowProps;return[r.default.createElement("ul",{key:"tabs",className:"tabs","data-tabs":!0,id:y},t.map(function(e,t){var n=e.kingdom;return r.default.createElement("li",{key:t,className:"tabs-title".concat(0===t?" is-active":""),style:{textTransform:"capitalize"}},r.default.createElement("a",{href:"#".concat(n)},n))})),r.default.createElement("div",{key:"tabs-content",className:"tabs-content","data-tabs-content":y},t.map(function(e,t){var o=e.kingdom,a=e.cards;return r.default.createElement("div",{key:t,className:"tabs-panel".concat(0===t?" is-active":""),id:o},r.default.createElement(u.CarouselCardsRow,s({cards:a},n)))}))]}},{key:"componentDidMount",value:function(){this.props.onComponentDidMount()}}])&&f(n.prototype,o),a&&f(n,a),t}();m.propTypes={speciesSummary:a.default.arrayOf(a.default.shape({kingdom:a.default.string.isRequired,cards:u.CarouselCardsRow.propTypes.cards})).isRequired,onComponentDidMount:a.default.func,carouselCardsRowProps:a.default.object},m.defaultProps={onComponentDidMount:function(){},responsiveCardsRowProps:{}};var b=m;t.default=b}},[[267,0]]]);