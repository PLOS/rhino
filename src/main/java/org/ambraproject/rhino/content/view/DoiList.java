package org.ambraproject.rhino.content.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DoiList {

  private final Collection<String> dois;

  public DoiList(Collection<String> dois) {
    this.dois = Collections.unmodifiableCollection(dois);
  }

  @VisibleForTesting
  public Collection<String> getDois() {
    return dois;
  }

  /*
   * Prefer TypeAdapter over JsonSerializer where we can get away with it, because TypeAdapter streams.
   */
  public static final TypeAdapter<DoiList> ADAPTER = new TypeAdapter<DoiList>() {
    @Override
    public void write(JsonWriter out, DoiList value) throws IOException {
      out.beginObject();
      for (String doi : value.dois) {
        out.name(ArticleIdentity.removeScheme(doi));
        out.beginObject();
        out.name(ArticleJsonConstants.MemberNames.DOI);
        out.value(doi);
        out.endObject();
      }
      out.endObject();
    }

    @Override
    public DoiList read(JsonReader in) throws IOException {
      List<String> buffer = Lists.newArrayList();
      in.beginObject();
      while (in.hasNext()) {
        in.nextName();
        in.beginObject();
        String doi = null;
        while (in.hasNext()) {
          if (ArticleJsonConstants.MemberNames.DOI.equals(in.nextName())) {
            doi = in.nextString();
          } else {
            in.skipValue();
          }
        }
        if (doi != null) {
          buffer.add(doi);
        }
        in.endObject();
      }
      in.endObject();
      return new DoiList(buffer);
    }
  };

}
