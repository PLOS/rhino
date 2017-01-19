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

package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class IngestionServiceTest {

  private IngestionService ingestionService;

  @BeforeMethod
  public void init() {
    ingestionService = new IngestionService();
  }

  private static Archive createStubArchive(byte[] manifestXml, Collection<String> entryNames) {
    Map<String, ByteSource> fileMap = new HashMap<>();

    for (String entryName : entryNames) {
      fileMap.put(entryName, ByteSource.empty());
    }

    if (manifestXml != null) {
      fileMap.put("manifest.xml", ByteSource.wrap(manifestXml));
    }

    return Archive.pack("test", fileMap);
  }

  private static ArticleItem createStubArticleItem() {
    ArticleItem articleItem = new ArticleItem();
    ArticleIngestion articleIngestion = new ArticleIngestion();
    Article article = new Article();
    article.setDoi("test");
    articleItem.setIngestion(articleIngestion);
    articleIngestion.setArticle(article);
    return articleItem;
  }

  @DataProvider
  private Iterator<Object[]> getPackageEntryNames() {

    Object[][] cases = new Object[][]{
        {getBaseEntryNames()}
    };
    return Stream.of(cases).map(c -> {
      Collection<String> entryNames = (Collection<String>) c[0];
      return new Object[]{entryNames};
    }).iterator();
  }

  private Collection<String> getBaseEntryNames() {
    Collection<String> entryNames = new ArrayList<>();
    entryNames.add("manifest.xml");
    entryNames.add("pone.0000001.xml");
    entryNames.add("pone.0000001.g001.PNG");
    entryNames.add("pone.0000001.g001.tif");
    entryNames.add("pone.0000001.g001.PNG_I");
    entryNames.add("pone.0000001.g001.PNG_L");
    entryNames.add("pone.0000001.g001.PNG_M");
    entryNames.add("pone.0000001.g001.PNG_S");
    return entryNames;
  }

  @Test(dataProvider = "getPackageEntryNames", expectedExceptions = RestClientException.class,
      expectedExceptionsMessageRegExp = ".*Invalid XML: Premature end of file.")
  public void testGetManifestXml(Collection<String> entryNames) throws Exception {
    Archive invalidTestArchive = createStubArchive(new byte[]{}, entryNames);
    ingestionService.getManifestXml(invalidTestArchive);
  }

  @Test(dataProvider = "getPackageEntryNames", expectedExceptions = RestClientException.class,
      expectedExceptionsMessageRegExp = "Archive has no manifest file")
  public void testGetManifestXml_missingManifest(Collection<String> entryNames) throws Exception {
    entryNames.remove("manifest.xml");
    Archive invalidTestArchive = createStubArchive(null, entryNames);
    ingestionService.getManifestXml(invalidTestArchive);
  }

  @Test
  public void testValidateManuscript() {
    ingestionService.validateManuscript(Doi.create("test"), "test");
  }

  @Test(expectedExceptions = RestClientException.class)
  public void testValidateManuscript_invalid() {
    ingestionService.validateManuscript(Doi.create("test"), "test2");
  }

  @Test
  public void testValidateAssetUniqueness() {
    ingestionService.validateAssetUniqueness(Doi.create("test"),
        ImmutableList.of(createStubArticleItem()));
  }

  @Test(expectedExceptions = RestClientException.class)
  public void testValidateAssetUniqueness_invalid() {
    ingestionService.validateAssetUniqueness(Doi.create("test2"),
        ImmutableList.of(createStubArticleItem()));
  }

}