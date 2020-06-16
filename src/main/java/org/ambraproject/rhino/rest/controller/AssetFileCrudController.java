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

package org.ambraproject.rhino.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import com.wordnik.swagger.annotations.ApiImplicitParam;

import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.model.ArticleFileStorage;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AssetFileCrudController extends RestController {
  @Autowired
  private ArticleCrudService articleCrudService;

  private void serve(HttpServletRequest request, HttpServletResponse response, ArticleFileStorage objMeta)
      throws IOException {
    objMeta.getContentType().ifPresent((String contentType) -> {
      response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    });

    objMeta.getDownloadName().ifPresent((String downloadName) -> {
      String contentDisposition = "attachment; filename=" + downloadName;
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
    });

    Timestamp timestamp = objMeta.getTimestamp();
    setLastModifiedHeader(response, timestamp);
    if (!checkIfModifiedSince(request, timestamp)) {
      response.setStatus(HttpStatus.NOT_MODIFIED.value());
      return;
    }

    try (InputStream fileStream = articleCrudService.getInputStream(objMeta);
         OutputStream responseStream = response.getOutputStream()) {
      ByteStreams.copy(fileStream, responseStream);
    }
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{articleDoi}/ingestions/{number}/items/{itemDoi}/files/{filetype}",
      params = "download", method = RequestMethod.GET)
  @ApiImplicitParam(name = "download", value = "download flag (any value)", required = true,
      defaultValue = "download", paramType = "query", dataType = "string")
  public void serveFile(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("articleDoi") String articleDoi,
                        @PathVariable("number") int ingestionNumber,
                        @PathVariable("itemDoi") String itemDoi,
                        @PathVariable("filetype") String fileType)
      throws IOException {
    ArticleFileIdentifier fileId = ArticleFileIdentifier.create(DoiEscaping.unescape(itemDoi), ingestionNumber, fileType);
    // TODO: Validate that articleDoi belongs to item's parent

    ArticleFileStorage objectMetadata = articleCrudService.getArticleItemFile(fileId);
    serve(request, response, objectMetadata);
  }

}
