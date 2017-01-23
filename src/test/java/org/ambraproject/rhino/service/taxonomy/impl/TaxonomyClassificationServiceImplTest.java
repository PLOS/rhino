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
package org.ambraproject.rhino.service.taxonomy.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceInvalidBehaviorException;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Alex Kudlick Date: 7/3/12
 */
public class TaxonomyClassificationServiceImplTest {
  @Autowired
  protected TaxonomyClassificationService taxonomyClassificationService;

  private DocumentBuilder documentBuilder;

  private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    if (documentBuilder != null) return documentBuilder;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    try {
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return documentBuilder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private Document getSampleArticle(String filename) {
    File file = new File("src/test/resources/articles/", filename);
    try {
      return getDocumentBuilder().parse(file);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAppendElementIfExists() throws Exception {
    Document article = getSampleArticle("pone.0048915.xml");
    StringBuilder sb = new StringBuilder();
    assertFalse(TaxonomyClassificationServiceImpl.appendElementIfExists(sb, article, "elementThatShouldntExist"));
    assertTrue(sb.toString().isEmpty());

    assertTrue(TaxonomyClassificationServiceImpl.appendElementIfExists(sb, article, "article-title"));
    String s = sb.toString();
    assertTrue(s.startsWith("Maternal Deprivation Exacerbates the Response to a High Fat Diet"));

    sb = new StringBuilder();
    assertTrue(TaxonomyClassificationServiceImpl.appendElementIfExists(sb, article, "abstract"));
    s = sb.toString().trim();
    assertTrue(s.startsWith(
        "Maternal deprivation (MD) during neonatal life has diverse long-term effects"));
  }

  @Test
  public void testGetCategorizationContent() throws Exception {

    // Arbitrary minimum number of characters that we should be sending for categorization.
    // This should be longer than the article title.
    int threshold = 500;
    Document article = getSampleArticle("pone.0048915.xml");
    String content = TaxonomyClassificationServiceImpl.getCategorizationContent(article);
    assertTrue(content.length() > threshold);

    // Editorial without an abstract, materials/methods, or results section.
    article = getSampleArticle("pntd.0001008.xml");
    content = TaxonomyClassificationServiceImpl.getCategorizationContent(article);
    assertTrue(content.length() > threshold);

    // Research article with non-standard section titles.
    article = getSampleArticle("pone.0040598.xml");
    content = TaxonomyClassificationServiceImpl.getCategorizationContent(article);

    // Call it good if we have material that's at least twice as long as the abstract.
    assertTrue(content.length()
        > article.getElementsByTagName("abstract").item(0).getTextContent().length() * 2);

    // Article with a very short, one-sentence "TOC" abstract that we don't even
    // display in ambra.
    article = getSampleArticle("pbio.0020302.xml");
    content = TaxonomyClassificationServiceImpl.getCategorizationContent(article);
    assertTrue(content.length() > threshold);
  }

  @Test
  public void testParseVectorElement() throws Exception {
    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/Biology and life sciences/Computational biology/Computational neuroscience/Single neuron function|(5) neuron*(5)</TERM>"),
        new WeightedTerm(
            "/Biology and life sciences/Computational biology/Computational neuroscience/Single neuron function"
            , 5));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/Medicine and health sciences/Anesthesiology/Anesthesia|(5) anesthesia(5)</TERM>"),
        new WeightedTerm(
            "/Medicine and health sciences/Anesthesiology/Anesthesia"
            , 5));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/Medicine and health sciences/Geriatrics/Frailty|(19) frailty(18) frail*(1)</TERM>"),
        new WeightedTerm(
            "/Medicine and health sciences/Geriatrics/Frailty"
            , 19));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/Biology and life sciences/Anatomy/Head/Face/Nose|(311) nose(311)</TERM>"),
        new WeightedTerm(
            "/Biology and life sciences/Anatomy/Head/Face/Nose"
            , 311));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/People and places/Demography|(7) demographics(7)</TERM>"),
        new WeightedTerm(
            "/People and places/Demography"
            , 7));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM>/Medicine and health sciences/Neurology/Cognitive neurology|(2) cognit*(2)</TERM>"),
        new WeightedTerm(
            "/Medicine and health sciences/Neurology/Cognitive neurology"
            , 2));

    assertEquals(TaxonomyClassificationServiceImpl.parseVectorElement(
            "<TERM> /Medicine and health sciences/Neurology/Cognitive neurology| (67) cognit*(2)</TERM>"),
        new WeightedTerm(
            "/Medicine and health sciences/Neurology/Cognitive neurology"
            , 67));
  }

  @Test(expectedExceptions = TaxonomyRemoteServiceInvalidBehaviorException.class)
  public void testInvalidVectorElement() throws Exception {
    // This appears to be a bug in the AI server--it sometimes does not return an
    // absolute path to a top-level category.  In these cases, the returned value
    // should be discarded.
    TaxonomyClassificationServiceImpl.parseVectorElement(
        "<TERM>Background noise (acoustics)|(1) background noise(1)</TERM>");
  }


  @DataProvider
  public Iterator<Object[]> getDistinctLeafNodesTestCases() {
    Object[][] cases = new Object[][]{
        {0, new WeightedTerm[]{}, new WeightedTerm[]{}},
        {1, new WeightedTerm[]{}, new WeightedTerm[]{}},
        {1, new WeightedTerm[]{new WeightedTerm("/a", 1)}, new WeightedTerm[]{new WeightedTerm("/a", 1)}},
        {2, new WeightedTerm[]{new WeightedTerm("/a", 1)}, new WeightedTerm[]{new WeightedTerm("/a", 1)}},

        {2,
            new WeightedTerm[]{
                new WeightedTerm("/a", 2),
                new WeightedTerm("/b", 3),
                new WeightedTerm("/c", 1),
            },
            new WeightedTerm[]{
                new WeightedTerm("/b", 3),
                new WeightedTerm("/a", 2),
            }
        },

        // Ensure that sort is stable on equal weights
        {2,
            new WeightedTerm[]{new WeightedTerm("/a", 2), new WeightedTerm("/b", 2), new WeightedTerm("/c", 1),},
            new WeightedTerm[]{new WeightedTerm("/a", 2), new WeightedTerm("/b", 2),}
        },
        {2,
            new WeightedTerm[]{new WeightedTerm("/b", 2), new WeightedTerm("/a", 2), new WeightedTerm("/c", 1),},
            new WeightedTerm[]{new WeightedTerm("/b", 2), new WeightedTerm("/a", 2),}
        },

        {2,
            new WeightedTerm[]{
                new WeightedTerm("/a/x", 6),
                new WeightedTerm("/b/x", 5),
                new WeightedTerm("/c/y", 4),
                new WeightedTerm("/d/y", 3),
                new WeightedTerm("/e/z", 2),
                new WeightedTerm("/f/z", 1),
            },
            new WeightedTerm[]{
                new WeightedTerm("/a/x", 6),
                new WeightedTerm("/b/x", 5),
                new WeightedTerm("/c/y", 4),
                new WeightedTerm("/d/y", 3),
            }
        },

        {2,
            new WeightedTerm[]{
                new WeightedTerm("/a/x", 6),
                new WeightedTerm("/b/x", 5),
                new WeightedTerm("/c/y", 4),
                new WeightedTerm("/d/y", 3),
                new WeightedTerm("/e/z", 2),
                new WeightedTerm("/f/x", 1),
            },
            new WeightedTerm[]{
                new WeightedTerm("/a/x", 6),
                new WeightedTerm("/b/x", 5),
                new WeightedTerm("/c/y", 4),
                new WeightedTerm("/d/y", 3),
                new WeightedTerm("/f/x", 1),
            }
        },

    };
    return Stream.of(cases).map((Object[] values) -> {
      int leafCount = (Integer) values[0];
      List<WeightedTerm> input = ImmutableList.copyOf((WeightedTerm[]) values[1]);
      List<WeightedTerm> expected = ImmutableList.copyOf((WeightedTerm[]) values[2]);
      return new Object[]{leafCount, input, expected};
    }).iterator();
  }

  @Test(dataProvider = "getDistinctLeafNodesTestCases")
  public void testGetDistinctLeafNodes(int leafCount, List<WeightedTerm> input, List<WeightedTerm> expected) {
    List<WeightedTerm> actual = TaxonomyClassificationServiceImpl.getDistinctLeafNodes(leafCount, input);
    Assert.assertEquals(actual, expected);
  }

}
