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

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ArticleXmlTest extends BaseRhinoTest {

  private static final Object[][] ASSET_NODE_CASES = new Object[][]{
      new Object[] {asset("title", "description"), new AssetMetadata[] {asset("", "")}},
      new Object[] {asset("title", "description"), new AssetMetadata[] {asset("title", "")}},
      new Object[] {asset("title", "description"), new AssetMetadata[] {asset("", "description")}},
      new Object[] {asset("title", "description"),
          new AssetMetadata[] {asset("title", ""), asset("", "description")}},
      new Object[] {asset("title", ""), new AssetMetadata[] {asset("", "")}},
      new Object[] {asset("", "description"), new AssetMetadata[] {asset("", "")}}};

  private static AssetMetadata asset(String title, String description) {
    return new AssetMetadata("testAssetDoi", title, description);
  }

  @DataProvider
  public static Object[][] assetNodeCases() {
    return ASSET_NODE_CASES;
  }

  @Test
  @UseDataProvider("assetNodeCases")
  public void testDisambiguateAssetNodes(AssetMetadata goodNode, AssetMetadata[] badNodes) {
    ImmutableList<AssetMetadata> cases = ImmutableList.<AssetMetadata>builder().add(goodNode).add(badNodes).build();
    assertEquals(ArticleXml.disambiguateAssetNodes(cases), goodNode);
    assertEquals(ArticleXml.disambiguateAssetNodes(cases.reverse()), goodNode);
  }

  @DataProvider
  public static Object[][] ambiguousAssetNodeCases() {
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
      .toArray(Object[][]::new);

  }

  @Test(expected = XmlContentException.class)
  @UseDataProvider("ambiguousAssetNodeCases")
  public void testAmbiguousAssetNodes(Collection<AssetMetadata> nodes) {
    ArticleXml.disambiguateAssetNodes(nodes);
  }

  @Test
  public void testParseRelatedArticles() throws IOException {
    ArticleXml xml = new ArticleXml(parseTestFile("pcbi.0020083.xml"));
    List<RelatedArticleLink> related = xml.parseRelatedArticles();
    assertEquals(1, related.size());
    assertEquals("companion", related.get(0).getType());
    assertEquals("friend", related.get(0).getSpecificUse());
  }
}
