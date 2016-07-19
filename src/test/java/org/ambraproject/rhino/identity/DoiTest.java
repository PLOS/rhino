package org.ambraproject.rhino.identity;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Stream;

public class DoiTest {

  private static class StylePair {
    private final Doi.UriStyle from;
    private final Doi.UriStyle to;

    private StylePair(Doi.UriStyle from, Doi.UriStyle to) {
      this.from = from;
      this.to = to;
    }
  }

  private static Stream<StylePair> getStyleMatrix() {
    return EnumSet.allOf(Doi.UriStyle.class).stream().flatMap(from ->
        EnumSet.allOf(Doi.UriStyle.class).stream().map(to -> new StylePair(from, to)));
  }

  @Test
  public void checkForPrefixCollisions() {
    getStyleMatrix().forEach(stylePair -> {
      if (stylePair.from != stylePair.to) {
        Assert.assertFalse(stylePair.from.getPrefix().startsWith(stylePair.to.getPrefix()));
      }
    });
  }

  @DataProvider
  public Iterator<Object[]> doiCases() {
    String[] doiNames = new String[]{"10.1371/foo"};
    return Stream.of(doiNames)
        .flatMap(doiName -> getStyleMatrix()
            .map(stylePair -> new Object[]{doiName, stylePair.from, stylePair.to}))
        .iterator();
  }

  @Test(dataProvider = "doiCases")
  public void testDoi(String doiName, Doi.UriStyle from, Doi.UriStyle to) {
    Doi fromRawName = Doi.create(doiName);
    URI asUri = fromRawName.asUri(from);
    Assert.assertEquals(asUri.toString(), from.getPrefix() + doiName);

    Doi fromUri = Doi.create(asUri.toString());
    Assert.assertEquals(fromUri, fromRawName);

    Doi converted = Doi.create(fromUri.asUri(to).toString());
    Assert.assertEquals(converted, fromUri);
    Assert.assertEquals(converted, fromRawName);
  }

}
