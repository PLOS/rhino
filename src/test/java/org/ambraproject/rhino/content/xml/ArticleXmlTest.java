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
    return Stream.of(cases).map((AssetMetadata[] metadataCases) -> {
      Object goodNode = metadataCases[0];
      Object[] badNodes = Arrays.copyOfRange(metadataCases, 1, metadataCases.length);
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
        {asset("title1", ""), asset("title2", "")},
        {asset("", "description1"), asset("", "description2")},
    };
    return Stream.of(cases)
        .flatMap((AssetMetadata[] nodes) -> {
          ImmutableList<AssetMetadata> nodeList = ImmutableList.copyOf(nodes);
          return Stream.of(nodeList, nodeList.reverse());
        })
        .map((Collection<AssetMetadata> nodeList) -> new Object[]{nodeList})
        .iterator();
  }

  @Test(dataProvider = "ambiguousAssetNodeCases", expectedExceptions = {XmlContentException.class})
  public void testAmbiguousAssetNodes(Collection<AssetMetadata> nodes) {
    ArticleXml.disambiguateAssetNodes(nodes);
  }

}
