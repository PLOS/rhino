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

import com.google.common.collect.ImmutableList;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.view.article.author.AuthorView;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AuthorsXmlExtractorTest extends BaseRhinoTest {
  @Autowired
  private XpathReader xpathReader;

  private static final File DATA_PATH = new File("src/test/resources/articles/");

  private static Document parseTestFile(String filename) throws IOException {
    File testFile = new File(DATA_PATH, filename);
    try (InputStream testData = new BufferedInputStream(new FileInputStream(testFile))) {
      return AmbraService.parseXml(testData);
    }
  }

  @Test
  public void testGetAuthors() throws Exception {
    /** TODO Change to use Data provider when we upgrade spring and we
     * no longer need the @RunWith annotation */
    for (Object[] o : getAuthorTestData()) {
      String filename = (String) o[0];
      AuthorView[] expected = (AuthorView[]) o[1];
      Document doc = parseTestFile(filename);
      List<AuthorView> actual = AuthorsXmlExtractor.getAuthors(doc, xpathReader);
      assertEquals(actual, Arrays.asList(expected));
    }
  }

  public static Object[][] getAuthorTestData() {
    return new Object[][]{
        {"pone.0005723.xml", new AuthorView[]{
            AuthorView.builder().setGivenNames("Jens L.").setSurnames("Franzen")
                .setAffiliations(ImmutableList.of(
                    "Forschungsinstitut Senckenberg, Frankfurt, Germany",
                    "Naturhistorisches Museum Basel, Basel, Switzerland"))
                .build(),
            AuthorView.builder().setGivenNames("Philip D.").setSurnames("Gingerich")
                .setAffiliations(ImmutableList.of("Museum of Paleontology and Department of Geological Sciences, University of Michigan, Ann Arbor, Michigan, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("J\u00F6rg").setSurnames("Habersetzer")
                .setAffiliations(ImmutableList.of("Forschungsinstitut Senckenberg, Frankfurt, Germany"))
                .build(),
            AuthorView.builder().setGivenNames("J\u00F8rn H.").setSurnames("Hurum")
                .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:j.h.hurum@nhm.uio.no\">j.h.hurum@nhm.uio.no</a>")
                .setAffiliations(ImmutableList.of("Natural History Museum, University of Oslo, Oslo, Norway")).build(),
            AuthorView.builder().setGivenNames("Wighart").setSurnames("von Koenigswald")
                .setAffiliations(ImmutableList.of("Steinmann-Institut für Geologie, Mineralogie und Paläontologie, Universität Bonn, Bonn, Germany"))
                .build(),
            AuthorView.builder().setGivenNames("B. Holly").setSurnames("Smith")
                .setAffiliations(ImmutableList.of("Museum of Anthropology, University of Michigan, Ann Arbor, Michigan, United States of America"))
                .build(),
        }},
        {"journal.pone.0032315.xml",
            new AuthorView[]{
                AuthorView.builder().setGivenNames("Peder").setSurnames("Fode")
                    .setAffiliations(ImmutableList.of("Department for Microbiological Surveillance and Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Anders Rhod").setSurnames("Larsen")
                    .setAffiliations(ImmutableList.of("Department for Microbiological Surveillance and Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Bjarke").setSurnames("Feenstra")
                    .setAffiliations(ImmutableList.of("Department of Epidemiological Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Cathrine").setSurnames("Jespersgaard")
                    .setAffiliations(ImmutableList.of("Department of Clinical Biochemistry and Immunology, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Robert Leo").setSurnames("Skov")
                    .setAffiliations(ImmutableList.of("Department for Microbiological Surveillance and Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Marc").setSurnames("Stegger")
                    .setAffiliations(ImmutableList.of("Department for Microbiological Surveillance and Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
                AuthorView.builder().setGivenNames("Vance G.").setSurnames("Fowler").setSuffix("Jr")
                    .setAffiliations(ImmutableList.of("Department of Infectious Diseases, Duke Medical Center, Durham, North Carolina, United States of America"))
                    .build(),
                AuthorView.builder().setGivenNames("the Danish SAB Study Group Consortium")
                    .setCustomFootnotes(ImmutableList.of("<p><span class=\"rel-footnote\">¶</span>Membership of the Danish SAB Study Group Consortium is provided in the Acknowledgments.</p>"))
                    .build(),
                AuthorView.builder().setGivenNames("Paal Skytt").setSurnames("Andersen")
                    .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:psa@ssi.dk\">psa@ssi.dk</a>")
                    .setAffiliations(ImmutableList.of("Department for Microbiological Surveillance and Research, Statens Serum Institut, Copenhagen, Denmark"))
                    .build(),
            }},
        {"journal.pone.0091831.xml",
            new AuthorView[]{
                AuthorView.builder().setGivenNames("Gulrez Shah").setSurnames("Azhar")
                    .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:gsazhar@iiphg.org\">gsazhar@iiphg.org</a>")
                    .setAffiliations(ImmutableList.of(
                        "Indian Institute of Public Health, Ahmedabad, Gujarat, India",
                        "Public Health Foundation of India, New Delhi, India"))
                    .build(),
                AuthorView.builder().setGivenNames("Dileep").setSurnames("Mavalankar")
                    .setAffiliations(ImmutableList.of(
                        "Indian Institute of Public Health, Ahmedabad, Gujarat, India",
                        "Public Health Foundation of India, New Delhi, India"))
                    .build(),
                AuthorView.builder().setGivenNames("Amruta").setSurnames("Nori-Sarma")
                    .setAffiliations(ImmutableList.of(
                        "Indian Institute of Public Health, Ahmedabad, Gujarat, India",
                        "Columbia Mailman School of Public Health, New York, New York, United States of America"))
                    .build(),
                AuthorView.builder().setGivenNames("Ajit").setSurnames("Rajiva")
                    .setAffiliations(ImmutableList.of("Indian Institute of Public Health, Ahmedabad, Gujarat, India"))
                    .build(),
                AuthorView.builder().setGivenNames("Priya").setSurnames("Dutta")
                    .setAffiliations(ImmutableList.of("Indian Institute of Public Health, Ahmedabad, Gujarat, India"))
                    .build(),
                AuthorView.builder().setGivenNames("Anjali").setSurnames("Jaiswal")
                    .setAffiliations(ImmutableList.of("Natural Resources Defense Council, New York, New York, United States of America"))
                    .build(),
                AuthorView.builder().setGivenNames("Perry").setSurnames("Sheffield")
                    .setAffiliations(ImmutableList.of("Icahn School of Medicine at Mount Sinai, New York, New York, United States of America"))
                    .build(),
                AuthorView.builder().setGivenNames("Kim").setSurnames("Knowlton")
                    .setAffiliations(ImmutableList.of(
                        "Columbia Mailman School of Public Health, New York, New York, United States of America",
                        "Natural Resources Defense Council, New York, New York, United States of America"))
                    .build(),
                AuthorView.builder().setGivenNames("Jeremy J.").setSurnames("Hess").setOnBehalfOf("on behalf of the Ahmedabad HeatClimate Study Group")
                    .setAffiliations(ImmutableList.of(
                        "Department of Emergency Medicine, Emory University School of Medicine, Atlanta, Georgia, United States of America",
                        "Department of Environmental Health, Emory University School of Public Health, Atlanta, Georgia, United States of America"))
                    .setCustomFootnotes(ImmutableList.of("<p>Membership of the Ahmedabad Heat and Climate Study Group is provided in the Acknowledgments</p>"))
                    .build(),
            }},
        {"journal.pone.0094497.xml",
            new AuthorView[]{
                AuthorView.builder().setGivenNames("Clementina E.").setSurnames("Cocuzza").setEqualContrib(true)
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Fabrizio").setSurnames("Piazza").setEqualContrib(true)
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Rosario").setSurnames("Musumeci")
                    .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:guido.cavaletti@unimibit\">guido.cavaletti@unimibit</a> (RM); <a href=\"mailto:guido.cavaletti@unimibit\">guido.cavaletti@unimibit</a> (GC)")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Davide").setSurnames("Oggioni")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Simona").setSurnames("Andreoni")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Margherita").setSurnames("Gardinetti")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Letizia").setSurnames("Fusco")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Maura").setSurnames("Frigo")
                    .setAffiliations(ImmutableList.of("Clinica Neurologica, A.O. S. Gerardo, Monza, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Paola").setSurnames("Banfi")
                    .setAffiliations(ImmutableList.of("Dipartimento di Neurologia, Ospedale di Circolo e Fondazione Macchi, Varese, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Maria R.").setSurnames("Rottoli")
                    .setAffiliations(ImmutableList.of("Centro Sclerosi Multipla, A.O. Ospedale Papa Giovanni XXIII, Bergamo, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Paolo").setSurnames("Confalonieri")
                    .setAffiliations(ImmutableList.of("U.O. Neurologia IV, Centro Sclerosi Multipla, Fondazione Istituto Neurologico “Carlo Besta”, Milano, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Monica").setSurnames("Rezzonico")
                    .setAffiliations(ImmutableList.of("Dipartimento di Neurologia, A.O Sant'Anna, Como, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Maria T.").setSurnames("Ferr\u00F2")
                    .setAffiliations(ImmutableList.of("Centro Sclerosi Multipla, Dipartimento di Neurologia, A.O. Ospedale Maggiore, Crema, Italy"))
                    .build(),
                AuthorView.builder().setGivenNames("Guido").setSurnames("Cavaletti").setOnBehalfOf("on behalf of the EBV-MS Italian Study Group")
                    .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:guido.cavaletti@unimibit\">guido.cavaletti@unimibit</a> (RM); <a href=\"mailto:guido.cavaletti@unimibit\">guido.cavaletti@unimibit</a> (GC)")
                    .setAffiliations(ImmutableList.of("Dipartimento di Chirurgia e Medicina Traslazionale, Università di Milano-Bicocca, Monza, Italy"))
                    .setCustomFootnotes(ImmutableList.of("<p>Membership of the EBV-MS Italian Study Group is provided in the Acknowledgments.</p>"))
                    .build(),
            }},
        {"journal.pone.0099781.xml",
            new AuthorView[]{
                AuthorView.builder().setGivenNames("Lucile").setSurnames("Mercadel")
                    .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:lucile.mercadal@psl.aphp.fr\">lucile.mercadal@psl.aphp.fr</a>")
                    .setAffiliations(ImmutableList.of(
                        "Inserm, Centre for research in Epidemiology and Population Health, U1018, Epidemiology of Diabetes, Obesity, and Kidney Diseases Team, Villejuif, France",
                        "Department of Nephrology, Hôpital Pitié-Salpêtrière, Assistance Publique-Hôpitaux de Paris, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Marie").setSurnames("Metzger")
                    .setAffiliations(ImmutableList.of(
                        "Inserm, Centre for research in Epidemiology and Population Health, U1018, Epidemiology of Diabetes, Obesity, and Kidney Diseases Team, Villejuif, France",
                        "Université Paris Sud 11, U1018, Villejuif, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Jean Philippe").setSurnames("Haymann")
                    .setAffiliations(ImmutableList.of("Department of Physiology and Nephrology, Hôpital Tenon, Assistance Publique-Hôpitaux de Paris, Université Pierre et Marie Curie, Inserm U702, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Eric").setSurnames("Thervet")
                    .setAffiliations(ImmutableList.of("Department of Nephrology, Hôpital Européen G Pompidou, Assistance Publique-hôpitaux de Paris, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Jean-Jacques").setSurnames("Boffa")
                    .setAffiliations(ImmutableList.of("Department of Nephrology, Hôpital Tenon, Assistance Publique-hôpitaux de Paris, université Pierre et Marie Curie, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Martin").setSurnames("Flamant")
                    .setAffiliations(ImmutableList.of(
                        "Department of Physiology, Hôpital Bichat, Assistance Publique-hôpitaux de Paris, Paris, France",
                        "Inserm U699, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Fran\u00E7ois").setSurnames("Vrtovsnik")
                    .setAffiliations(ImmutableList.of(
                        "Inserm U699, Paris, France",
                        "Department of Nephrology, Hôpital Bichat, Assistance Publique-hôpitaux de Paris, Université Pierre et Marie Curie, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Pascal").setSurnames("Houillier")
                    .setAffiliations(ImmutableList.of("Department of Physiology, Hôpital Européen G Pompidou, Assistance Publique-hôpitaux de Paris, Université Paris Descartes, Paris, France"))
                    .build(),
                AuthorView.builder().setGivenNames("Marc").setSurnames("Froissart")
                    .setAffiliations(ImmutableList.of("Inserm, Centre for research in Epidemiology and Population Health, U1018, Epidemiology of Diabetes, Obesity, and Kidney Diseases Team, Villejuif, France"))
                    .build(),
                AuthorView.builder().setGivenNames("B\u00E9n\u00E9dicte").setSurnames("Stengel").setOnBehalfOf("on behalf of the NephroTest Study Group")
                    .setAffiliations(ImmutableList.of(
                        "Inserm, Centre for research in Epidemiology and Population Health, U1018, Epidemiology of Diabetes, Obesity, and Kidney Diseases Team, Villejuif, France",
                        "Université Paris Sud 11, U1018, Villejuif, France"))
                    .setCustomFootnotes(ImmutableList.of("<p>Membership of the NephroTest Study Group is provided in the Acknowledgments..</p>"))
                    .build(),
            }},
        {"journal.pone.0055490.xml", new AuthorView[]{
            AuthorView.builder().setGivenNames("Michael P.").setSurnames("Heaton")
                .setEqualContrib(true)
                .setCorresponding( "<span class=\"email\">* E-mail:</span> <a href=\"mailto:mike.heaton@ars.usda.gov\">mike.heaton@ars.usda.gov</a> (MPH); <a href=\"mailto:ted.kalbfleisch@louisville.edu\">ted.kalbfleisch@louisville.edu</a> (TSK)")
                .setAffiliations(ImmutableList.of("U.S. Meat Animal Research Center (USMARC), Clay Center, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Theodore S.").setSurnames("Kalbfleisch")
                .setEqualContrib(true)
                .setCorresponding("<span class=\"email\">* E-mail:</span> <a href=\"mailto:mike.heaton@ars.usda.gov\">mike.heaton@ars.usda.gov</a> (MPH); <a href=\"mailto:ted.kalbfleisch@louisville.edu\">ted.kalbfleisch@louisville.edu</a> (TSK)")
                .setAffiliations(ImmutableList.of(
                    "Department of Biochemistry and Molecular Biology, School of Medicine, University of Louisville, Louisville, Kentucky, United States of America",
                    "Intrepid Bioinformatics, Louisville, Kentucky, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Dustin T.").setSurnames("Petrik")
                .setAffiliations(ImmutableList.of("GeneSeek, a Neogen company, Lincoln, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Barry").setSurnames("Simpson")
                .setAffiliations(ImmutableList.of("GeneSeek, a Neogen company, Lincoln, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("James W.").setSurnames("Kijas")
                .setAffiliations(ImmutableList.of("Division of Animal, Food and Health Sciences, CSIRO, Brisbane, Australia"))
                .build(),
            AuthorView.builder().setGivenNames("Michael L.").setSurnames("Clawson")
                .setAffiliations(ImmutableList.of("U.S. Meat Animal Research Center (USMARC), Clay Center, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Carol G.").setSurnames("Chitko-McKown")
                .setAffiliations(ImmutableList.of("U.S. Meat Animal Research Center (USMARC), Clay Center, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Gregory P.").setSurnames("Harhay")
                .setAffiliations(ImmutableList.of("U.S. Meat Animal Research Center (USMARC), Clay Center, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Kreg A.").setSurnames("Leymaster")
                .setAffiliations(ImmutableList.of("U.S. Meat Animal Research Center (USMARC), Clay Center, Nebraska, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("the International Sheep Genomics Consortium")
                .setCustomFootnotes(ImmutableList.of("<p><span class=\"rel-footnote\">¶</span>Membership of the International Sheep Genomics Consortium is provided in the Acknowledgments and at <ext-link xmlns:xlink=\"http://www.w3.org/1999/xlink\" ext-link-type=\"uri\" xlink:href=\"http://www.sheephapmap.org/participants.php\" xlink:type=\"simple\">http://www.sheephapmap.org/participants.php</ext-link>.</p>"))
                .build(),
        }},
        {"journal.pgen.0030208.xml", new AuthorView[]{
            AuthorView.builder().setGivenNames("Patrick J").setSurnames("Collins")
                .setAffiliations(ImmutableList.of("Department of Genetics, Stanford University School of Medicine, Stanford, California, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Yuya").setSurnames("Kobayashi")
                .setAffiliations(ImmutableList.of("Department of Genetics, Stanford University School of Medicine, Stanford, California, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Loan").setSurnames("Nguyen")
                .setAffiliations(ImmutableList.of("Department of Genetics, Stanford University School of Medicine, Stanford, California, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Nathan D").setSurnames("Trinklein")
                .setAffiliations(ImmutableList.of("Department of Genetics, Stanford University School of Medicine, Stanford, California, United States of America"))
                .build(),
            AuthorView.builder().setGivenNames("Richard M").setSurnames("Myers")
                .setAffiliations(ImmutableList.of("Department of Genetics, Stanford University School of Medicine, Stanford, California, United States of America"))
                .setCorresponding("<span class=\"email\">*</span>To whom correspondence should be addressed. E-mail: <a href=\"mailto:myers@shgc.stanford.edu\">myers@shgc.stanford.edu</a> ")
                .build()
        }}
    };
  }


  // Below: Utilities for representing AuthorView objects as Java constants.
  // May be used manually to generate regression test case code from known-to-be-good cases.

  private static String representAuthorView(AuthorView a) {
    StringBuilder sb = new StringBuilder();
    sb.append("AuthorView.builder()");

    representStringField(sb, "setGivenNames", a.getGivenNames());
    representStringField(sb, "setSurnames", a.getSurnames());
    representStringField(sb, "setSuffix", a.getSuffix());
    representStringField(sb, "setOnBehalfOf", a.getOnBehalfOf());
    representStringField(sb, "setCorresponding", a.getCorresponding());
    representBooleanField(sb, "setEqualContrib", a.getEqualContrib());
    representBooleanField(sb, "setDeceased", a.getDeceased());
    representBooleanField(sb, "setRelatedFootnote", a.getRelatedFootnote());
    representStringListField(sb, "setCurrentAddresses", a.getCurrentAddresses());
    representStringListField(sb, "setAffiliations", a.getAffiliations());
    representStringListField(sb, "setCustomFootnotes", a.getCustomFootnotes());

    sb.append(".build(),");
    return sb.toString();
  }

  private static void representStringField(StringBuilder sb, String setterName, String value) {
    if (value != null) {
      sb.append(String.format(".%s(\"%s\")", setterName, StringEscapeUtils.escapeJava(value)));
    }
  }

  private static void representBooleanField(StringBuilder sb, String setterName, boolean value) {
    if (value) {
      sb.append(String.format(".%s(%b)", setterName, value));
    }
  }

  private static void representStringListField(StringBuilder sb, String setterName, List<String> value) {
    if (value != null && !value.isEmpty()) {
      sb.append(String.format("\n.%s(ImmutableList.of(", setterName));
      if (value.size() > 1) {
        sb.append('\n');
      }
      for (Iterator<String> iterator = value.iterator(); iterator.hasNext(); ) {
        sb.append('"').append(iterator.next()).append('"');
        sb.append(iterator.hasNext() ? ",\n" : "))");
      }
    }
  }

}
