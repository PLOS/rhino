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

package org.ambraproject.rhino.content.xml;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class ArticleXmlTest {

  private static AssetMetadata asset(String title, String description) {
    return new AssetMetadata("testAssetDoi", title, description);
  }

  @DataProvider
  public Iterator<Object[]> assetNodeCases() {
    AssetMetadata[][] cases = {
        {asset("title", "description"), asset("", "")},
        {asset("title", "description"), asset("title", "")},
        {asset("title", "description"), asset("", "description")},
        {asset("title", "description"), asset("title", ""), asset("", "description")},
        {asset("title", ""), asset("", "")},
        {asset("", "description"), asset("", "")},
    };
    return Stream.of(cases).<Object[]>map((AssetMetadata[] metadataCases) -> {
      AssetMetadata goodNode = metadataCases[0];
      AssetMetadata[] badNodes = Arrays.copyOfRange(metadataCases, 1, metadataCases.length);
      return new Object[]{goodNode, badNodes};
    }).iterator();
  }

  @Test(dataProvider = "assetNodeCases")
  public void testDisambiguateAssetNodes(AssetMetadata goodNode, AssetMetadata[] badNodes) {
    ImmutableList<AssetMetadata> cases = ImmutableList.<AssetMetadata>builder().add(goodNode).add(badNodes).build();
    assertEquals(ArticleXml.disambiguateAssetNodes(cases), goodNode);
    assertEquals(ArticleXml.disambiguateAssetNodes(cases.reverse()), goodNode);
  }

  @DataProvider
  public Iterator<Object[]> ambiguousAssetNodeCases() {
    AssetMetadata[][] cases = {
        {asset("title", ""), asset("", "description")},
        {asset("title1", "description1"), asset("title2", "description2")},
        {asset("title1", "description1"), asset("title2", "description2"), asset("title3", "description3")},
        {asset("title1", "description1"), asset("title2", "")},
        {asset("title1", "description1"), asset("", "description2")},
        {asset("title1", "description1"), asset("title2", ""), asset("", "description3")},
        {asset("title", "description1"), asset("title", "description2"), asset("title", "")},
        {asset("title1", "description"), asset("title2", "description"), asset("", "description")},
        {asset("title1", ""), asset("title2", "")},
        {asset("", "description1"), asset("", "description2")},
    };
    return Stream.of(cases)
        .<ImmutableList<AssetMetadata>>flatMap((AssetMetadata[] nodes) -> {
          ImmutableList<AssetMetadata> nodeList = ImmutableList.copyOf(nodes);
          return Stream.of(nodeList, nodeList.reverse());
        })
        .<Object[]>map((Collection<AssetMetadata> nodeList) -> new Object[]{nodeList})
        .iterator();
  }

  @Test(dataProvider = "ambiguousAssetNodeCases", expectedExceptions = {XmlContentException.class})
  public void testAmbiguousAssetNodes(Collection<AssetMetadata> nodes) {
    ArticleXml.disambiguateAssetNodes(nodes);
  }

}
