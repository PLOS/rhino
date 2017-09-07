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

package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.rest.RestClientException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ArticlePackage {

  private final ManifestXml manifest;
  private final ArticleItemInput articleItem;
  private final ImmutableList<ArticleItemInput> allItems;
  private final ImmutableList<ArticleFileInput> ancillaryFiles;
  private final String bucketName;

  ArticlePackage(ArticleItemInput articleItem, List<ArticleItemInput> assetItems,
                 List<ArticleFileInput> ancillaryFiles, ManifestXml manifest,
                 String bucketName) {
    this.articleItem = Objects.requireNonNull(articleItem);
    this.allItems = ImmutableList.<ArticleItemInput>builder()
        .add(articleItem).addAll(assetItems).build();
    this.ancillaryFiles = ImmutableList.copyOf(ancillaryFiles);
    this.manifest = manifest;
    this.bucketName = bucketName;
  }

  public void validateAssetCompleteness(Set<Doi> manuscriptDois) {
    Set<Doi> packageDois = getAllItems().stream()
        .map(ArticleItemInput::getDoi)
        .collect(Collectors.toSet());
    Set<Doi> missingDois = Sets.difference(manuscriptDois, packageDois);
    if (!missingDois.isEmpty()) {
      String message = "Asset DOIs mentioned in manuscript are not included in package: "
          + missingDois.stream().map(Doi::getName).sorted().collect(Collectors.toList());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  public Doi getDoi() {
    return articleItem.getDoi();
  }

  public ImmutableList<ArticleItemInput> getAllItems() {
    return allItems;
  }

  public ImmutableList<ArticleFileInput> getAncillaryFiles() {
    return ancillaryFiles;
  }

  public ManifestXml getManifest() {
    return manifest;
  }

  public String getBucketName() {
    return bucketName;
  }
}
