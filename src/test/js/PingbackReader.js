/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

var DOI_SCHEME = 'info:doi/';
var SERVER_ROOT = 'http://localhost:8080/';

function main() {
  $('#jsWarning').hide();
  $.ajax({
    url: SERVER_ROOT + 'articles?pingbacks',
    dataType: 'jsonp',
    success: function (data, textStatus, jqXHR) {
      populateArticleTable(data);
    },
    error: function (jqXHR, textStatus, errorThrown) {
      alert(textStatus);
    },
    complete: function (jqXHR, textStatus) {
    }
  });
}

function populateArticleTable(pingbacksByArticle) {
  var articleTable = $('table.articles');
  $.each(pingbacksByArticle, function (index, article) {
    var articleRow = $('<tr/>');
    var fetchCell = $('<td/>').addClass('fetch').attr('colspan', 5);
    var fetchRow = $('<tr/>').append(fetchCell).hide();
    articleRow
      .append($('<td/>').text(article.doi))
      .append($('<td/>').html($('<a/>').attr('href', article.articleUrl).text(article.title)))
      .append($('<td/>').text(article.pingbackCount))
      .append($('<td/>').text(article.mostRecentPingback))
      .append($('<td/>').append(makeFetchButton(article, articleRow, fetchRow)))
    articleTable.append(articleRow).append(fetchRow);
  });
}

function makeFetchButton(article, articleRow, fetchRow) {
  var button = $('<button/>').text('Fetch');
  button.click(function () {
    button.attr('disabled', true);
    fetchRow.show().find('.fetch').html(fetchPingbacks(article));
  });
  return button;
}

function doiAsIdentifier(doi) {
  return (doi.substr(0, DOI_SCHEME.length) === DOI_SCHEME) ? doi.substr(DOI_SCHEME.length) : doi;
}

function fetchPingbacks(article) {
  var headerText = "Pingbacks for \"" + article.title + "\"";
  var fetchBox = $('<span/>');
  fetchBox.append($('<h3/>').text(headerText));
  $.ajax({
    url: SERVER_ROOT + 'articles/' + doiAsIdentifier(article.doi) + '?pingbacks',
    dataType: 'jsonp',
    success: function (data, textStatus, jqXHR) {
      populatePingbacks(fetchBox, data);
    },
    error: function (jqXHR, textStatus, errorThrown) {
      alert(textStatus);
    },
    complete: function (jqXHR, textStatus) {
    }
  });
  return fetchBox;
}

function populatePingbacks(box, pingbacksList) {
  var table = $('table.pingbacks.prototype').clone().removeClass('prototype').show();
  $.each(pingbacksList, function (index, pingback) {
    var row = $('<tr/>')
      .append($('<td/>').text(pingback.title))
      .append($('<td/>').html($('<a/>').attr('href', pingback.url).text(pingback.url)))
      .append($('<td/>').text(pingback.created))
    table.append(row);
  });
  box.append(table);
  return box;
}

$(document).ready(main);
