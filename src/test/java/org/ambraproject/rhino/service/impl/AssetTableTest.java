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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.input.RepoCollectionInput;
import org.plos.crepo.model.metadata.RepoCollectionList;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.model.identity.RepoVersion;
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
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class AssetTableTest {

  @DataProvider
  public Object[][] ingestibles() {
    File[] ingestibles = new File("src/test/resources/articles/").listFiles((dir, name) -> name.endsWith(".zip"));
    return Lists.transform(Arrays.asList(ingestibles), file -> {
      try {
        Archive archive = Archive.readZipFileIntoMemory(file);
        ManifestXml manifest = new ManifestXml(parseFrom(archive, "MANIFEST.xml"));
        String articleEntryName = manifest.getArticleAsset().getRepresentation("manuscript").get().getFile().getEntry();
        ArticleXml article = new ArticleXml(parseFrom(archive, articleEntryName));
        return new Object[]{manifest, article};
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).toArray(new Object[0][]);
  }

  private static Document parseFrom(Archive archive, String entryName) throws IOException {
    try (InputStream stream = archive.openFile(entryName)) {
      return AmbraService.newDocumentBuilder().parse(stream);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

//  @Test(dataProvider = "ingestibles", enabled = false)
//  public void test(ManifestXml manifest, ArticleXml article) throws IOException {
//    AssetTable<String> assetTable = AssetTable.buildFromIngestible(article.findAllAssetNodes(), manifest);
//    assertFalse(assetTable.getAssets().isEmpty());
//
//    BiMap<String, RepoVersion> dummyObjects = createDummyRepoObjects(assetTable);
//    Map<String, Object> assetMetadata = assetTable.buildAsAssetMetadata(dummyObjects);
//
//    RepoCollectionList dummyCollection = createDummyRepoCollection(assetMetadata, dummyObjects.values());
//    AssetTable<RepoVersion> rebuilt = AssetTable.buildFromAssetMetadata(dummyCollection);
//    assertEqualAssetTables(rebuilt, assetTable, false);
//
//    // Test buildAsAssetMetadata once more on an AssetTable<RepoVersion>
//    Map<String, Object> rebuiltMetadata = rebuilt.buildAsAssetMetadata(Maps.asMap(dummyObjects.values(), Functions.<RepoVersion>identity()));
//    AssetTable<RepoVersion> rebuiltMetadataResult = AssetTable.buildFromAssetMetadata(
//        createDummyRepoCollection(rebuiltMetadata, dummyObjects.values()));
//    assertEqualAssetTables(rebuiltMetadataResult, assetTable, false);
//    assertEqualAssetTables(rebuiltMetadataResult, rebuilt, true);
//  }
//
//  private static final byte[] DUMMY_CONTENT = new byte[]{0};
//  private final InMemoryContentRepoService inMemoryContentRepoService = new InMemoryContentRepoService("defaultBucket");
//
//  private ImmutableBiMap<String, RepoVersion> createDummyRepoObjects(AssetTable<String> assetTable) {
//    ImmutableBiMap.Builder<String, RepoVersion> dummyRepoVersions = ImmutableBiMap.builder();
//    for (AssetTable.Asset<String> asset : assetTable.getAssets()) {
//      String key = asset.getFileLocator();
//      RepoObjectMetadata dummyObject = inMemoryContentRepoService.autoCreateRepoObject(
//          RepoObjectInput.builder(mangle(key)).byteContent(DUMMY_CONTENT).build());
//      dummyRepoVersions.put(key, dummyObject.getVersion());
//    }
//    return dummyRepoVersions.build();
//  }

  // Create a meaningless but consistent key
  private static String mangle(String key) {
    return Hashing.sha1().newHasher().putLong(5842999961261284079L).putString(key, Charsets.UTF_8).hash().toString();
  }

//  private RepoCollectionList createDummyRepoCollection(Map<String, Object> assetMetadata, Collection<RepoVersion> dummyObjects) {
//    Map<String, Map<String, Object>> userMetadata = ImmutableMap.of("assets", assetMetadata);
//    return inMemoryContentRepoService.autoCreateCollection(RepoCollection.builder()
//        .setKey("test")
//        .setObjects(dummyObjects)
//        .setUserMetadata(new Gson().toJson(userMetadata))
//        .build());
//  }
//
//  private static void assertEqualAssetTables(AssetTable<?> actual, AssetTable<?> expected, boolean compareFileLocators) {
//    Ordering<AssetTable.Asset<?>> assetOrdering = new Ordering<AssetTable.Asset<?>>() {
//      @Override
//      public int compare(AssetTable.Asset<?> o1, AssetTable.Asset<?> o2) {
//        int cmp = o1.getIdentity().getIdentifier().compareTo(o2.getIdentity().getIdentifier());
//        if (cmp != 0) return cmp;
//        return o1.getFileType().compareTo(o2.getFileType());
//      }
//    };
//    List<? extends AssetTable.Asset<?>> actualAssets = assetOrdering.immutableSortedCopy(actual.getAssets());
//    List<? extends AssetTable.Asset<?>> expectedAssets = assetOrdering.immutableSortedCopy(expected.getAssets());
//    assertEquals(actualAssets.size(), expectedAssets.size());
//    for (int i = 0; i < actualAssets.size(); i++) {
//      AssetTable.Asset<?> actualAsset = actualAssets.get(i);
//      AssetTable.Asset<?> expectedAsset = expectedAssets.get(i);
//      assertEquals(actualAsset.getIdentity(), expectedAsset.getIdentity());
//      assertEquals(actualAsset.getFileType(), expectedAsset.getFileType());
//      assertEquals(actualAsset.getAssetType(), expectedAsset.getAssetType());
//      if (compareFileLocators) {
//        assertEquals(actualAsset.getFileLocator(), expectedAsset.getFileLocator());
//      }
//    }
//  }

}
