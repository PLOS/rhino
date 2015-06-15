package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.util.Archive;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.testng.Assert.assertFalse;

public class AssetTableTest {

  @DataProvider
  public Object[][] ingestibles() {
    File[] ingestibles = new File("src/test/resources/articles/").listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".zip");
      }
    });
    return Lists.transform(Arrays.asList(ingestibles), new Function<File, Object[]>() {
      @Override
      public Object[] apply(File file) {
        try {
          Archive archive = Archive.readZipFileIntoMemory(file);
          ManifestXml manifest = new ManifestXml(parseFrom(archive, "MANIFEST.xml"));
          String articleEntryName = manifest.getArticleXml();
          ArticleXml article = new ArticleXml(parseFrom(archive, articleEntryName));
          return new Object[]{manifest, article};
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }).toArray(new Object[0][]);
  }

  private static Document parseFrom(Archive archive, String entryName) throws IOException {
    try (InputStream stream = archive.openFile(entryName)) {
      return IngestibleUtil.newDocumentBuilder().parse(stream);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  @Test(dataProvider = "ingestibles")
  public void test(ManifestXml manifest, ArticleXml article) throws IOException {
    AssetTable<String> assetTable = AssetTable.buildFromIngestible(article.findAllAssetNodes(), manifest);
    assertFalse(assetTable.getAssets().isEmpty());
  }

}
