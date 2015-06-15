package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.InMemoryContentRepoService;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.testng.Assert.assertEquals;
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

    Map<String, RepoVersion> dummyObjects = createDummyRepoObjects(assetTable);
    Map<String, Object> assetMetadata = assetTable.buildAsAssetMetadata(dummyObjects);

    RepoCollectionList dummyCollection = createDummyRepoCollection(assetMetadata, dummyObjects.values());
    AssetTable<RepoVersion> rebuilt = AssetTable.buildFromAssetMetadata(dummyCollection, article);
    assertEquals(assetTable.getAssetIdentities(), rebuilt.getAssetIdentities());
  }

  private static final byte[] DUMMY_CONTENT = new byte[]{0};
  private final InMemoryContentRepoService inMemoryContentRepoService = new InMemoryContentRepoService("defaultBucket");

  private Map<String, RepoVersion> createDummyRepoObjects(AssetTable<String> assetTable) {
    ImmutableMap.Builder<String, RepoVersion> dummyRepoVersions = ImmutableMap.builder();
    for (AssetTable.Asset<String> asset : assetTable.getAssets()) {
      String key = asset.getFileLocator();
      RepoObjectMetadata dummyObject = inMemoryContentRepoService.autoCreateRepoObject(
          new RepoObject.RepoObjectBuilder(key).byteContent(DUMMY_CONTENT).build());
      dummyRepoVersions.put(key, dummyObject.getVersion());
    }
    return dummyRepoVersions.build();
  }

  private RepoCollectionList createDummyRepoCollection(Map<String, Object> assetMetadata, Collection<RepoVersion> dummyObjects) {
    Map<String, Map<String, Object>> userMetadata = ImmutableMap.of("assets", assetMetadata);
    return inMemoryContentRepoService.autoCreateCollection(RepoCollection.builder()
        .setKey("test")
        .setObjects(dummyObjects)
        .setUserMetadata(new Gson().toJson(userMetadata))
        .build());
  }

}
