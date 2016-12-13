package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import org.ambraproject.rhino.BaseRhinoTransactionalTest;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class IngestionServiceTest extends BaseRhinoTransactionalTest {

  @Autowired
  private IngestionService ingestionService;

  @BeforeMethod
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  private Archive getTestArchive(byte[] manifestXml, ImmutableSet<String> entryNames) {
    Map<String, ByteSource> fileMap = new HashMap<>();

    for (String entryName : entryNames) {
      fileMap.put(entryName, ByteSource.empty());
    }

    if (manifestXml != null) {
      fileMap.put("manifest.xml", ByteSource.wrap(manifestXml));
    }

    return Archive.pack("test", fileMap);
  }

  private ImmutableSet<String> getValidEntryNames() {
    ImmutableSet.Builder<String> entryNameBuilder = new ImmutableSet.Builder<>();
    entryNameBuilder.add("manifest.xml");
    entryNameBuilder.add("pone.0000001.xml");
    entryNameBuilder.add("pone.0000001.g001.PNG");
    entryNameBuilder.add("pone.0000001.g001.tif");
    entryNameBuilder.add("pone.0000001.g001.PNG_I");
    entryNameBuilder.add("pone.0000001.g001.PNG_L");
    entryNameBuilder.add("pone.0000001.g001.PNG_M");
    entryNameBuilder.add("pone.0000001.g001.PNG_S");
    return entryNameBuilder.build();
  }

  private ImmutableSet<String> getInvalidEntryNames_MissingManifest() {
    ImmutableSet.Builder<String> entryNameBuilder = new ImmutableSet.Builder<>();
    entryNameBuilder.add("pone.0000001.xml");
    entryNameBuilder.add("pone.0000001.g001.PNG");
    entryNameBuilder.add("pone.0000001.g001.tif");
    entryNameBuilder.add("pone.0000001.g001.PNG_I");
    entryNameBuilder.add("pone.0000001.g001.PNG_L");
    entryNameBuilder.add("pone.0000001.g001.PNG_M");
    entryNameBuilder.add("pone.0000001.g001.PNG_S");
    return entryNameBuilder.build();
  }

  @Test
  public void testServiceAutowiring() {
    assertNotNull(ingestionService);
  }

  @Test
  public void testCreateIngestPackage_valid() throws Exception {

    Archive validTestArchive = getTestArchive(new byte[]{}, getValidEntryNames());
    ArticleIngestion articleIngestion = ingestionService.ingest(validTestArchive);

    assertNotNull(articleIngestion);
  }

  @Test
  public void testCreateIngestPackage_invalid_missingManifest() throws Exception {

    Archive invalidTestArchive = getTestArchive(null, getInvalidEntryNames_MissingManifest());
    try {
      ingestionService.ingest(invalidTestArchive);
    } catch (RestClientException e) {
      assertEquals(e.getResponseStatus(), HttpStatus.BAD_REQUEST);
      return;
    }
    fail("Expected RestClientException if no manifest file is included in archive");
  }

  @Test
  public void ingest() throws Exception {

  }

}