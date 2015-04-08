package org.ambraproject.rhino.service.impl;

import org.testng.annotations.Test;

import static org.ambraproject.rhino.service.impl.LegacyIngestionService.shouldSaveAssetFile;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LegacyIngestionServiceTest {

  @Test
  public void testShouldSaveAssetFile() {
    assertTrue(shouldSaveAssetFile("pone.0058631.g001.tif", "pone.0058631.xml"));
    assertTrue(shouldSaveAssetFile("ppat.1003193.g002.PNG_M", "ppat.1003193.xml"));
    assertTrue(shouldSaveAssetFile("pcbi.1002867.pdf", "pcbi.1002867.xml"));
    assertTrue(shouldSaveAssetFile("pone.0055746.s005.doc", "pone.0055746.xml"));

    assertFalse(shouldSaveAssetFile("manifest.dtd", "p.0.xml"));
    assertFalse(shouldSaveAssetFile("MANIFEST.xml", "p.0.xml"));
    assertFalse(shouldSaveAssetFile("pone.0058631.xml", "pone.0058631.xml"));
    assertFalse(shouldSaveAssetFile("ppat.1003188.xml.meta", "ppat.1003188.xml"));
    assertFalse(shouldSaveAssetFile("pone.0058631.xml.orig", "pone.0058631.xml"));
  }

}
