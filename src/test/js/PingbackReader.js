/*
 * Copyright (c) 2013 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var DOI_SCHEME = 'info:doi/';
var SERVER_ROOT = 'http://localhost:8080/';

function main() {
  $('#jsWarning').hide();
  $.ajax({
    url: SERVER_ROOT + 'article?linkbacks',
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

function populateArticleTable(linkbacksByArticle) {
  var articleTable = $('table.articles');
  $.each(linkbacksByArticle, function (index, article) {
    var articleRow = $('<tr/>');
    var fetchCell = $('<td/>').addClass('fetch').attr('colspan', 5);
    var fetchRow = $('<tr/>').append(fetchCell).hide();
    articleRow
      .append($('<td/>').text(article.doi))
      .append($('<td/>').text(article.title))
      .append($('<td/>').text(article.linkbackCount))
      .append($('<td/>').text(article.mostRecentLinkback))
      .append($('<td/>').append(makeFetchButton(article, articleRow, fetchRow)))
    articleTable.append(articleRow).append(fetchRow);
  });
}

function makeFetchButton(article, articleRow, fetchRow) {
  var button = $('<button/>').text('Fetch');
  button.click(function () {
    button.attr('disabled', true);
    fetchRow.show().find('.fetch').html(fetchLinkbacks(article));
  });
  return button;
}

function doiAsIdentifier(doi) {
  return (doi.substr(0, DOI_SCHEME.length) === DOI_SCHEME) ? doi.substr(DOI_SCHEME.length) : doi;
}

function fetchLinkbacks(article) {
  var headerText = "Linkbacks for \"" + article.title + "\"";
  var fetchBox = $('<span/>');
  fetchBox.append($('<h3/>').text(headerText));
  $.ajax({
    url: SERVER_ROOT + 'article/' + doiAsIdentifier(article.doi) + '?linkbacks',
    dataType: 'jsonp',
    success: function (data, textStatus, jqXHR) {
      populateLinkbacks(fetchBox, data);
    },
    error: function (jqXHR, textStatus, errorThrown) {
      alert(textStatus);
    },
    complete: function (jqXHR, textStatus) {
    }
  });
  return fetchBox;
}

function populateLinkbacks(box, linkbacksList) {
  var table = $('table.linkbacks.prototype').clone().removeClass('prototype').show();
  $.each(linkbacksList, function (index, linkback) {
    var row = $('<tr/>')
      .append($('<td/>').text(linkback.title))
      .append($('<td/>').text(linkback.url))
      .append($('<td/>').text(linkback.created))
      .append($('<td/>').text(linkback.blogName))
      .append($('<td/>').text(linkback.excerpt));
    table.append(row);
  });
  box.append(table);
  return box;
}

$(document).ready(main);
