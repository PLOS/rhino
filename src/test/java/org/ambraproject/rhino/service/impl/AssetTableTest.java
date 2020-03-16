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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.util.Archive;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class AssetTableTest {
  @DataProvider
  public static Object[][] ingestibles() {
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

  // Create a meaningless but consistent key
  private static String mangle(String key) {
    return Hashing.sha1().newHasher().putLong(5842999961261284079L).putString(key, Charsets.UTF_8).hash().toString();
  }
}
