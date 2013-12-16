package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import org.ambraproject.rhino.view.article.ArticleJsonConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Controller
public class RootController {

  private static final ImmutableList<String> SYNDICATION_STATUSES_LOWERCASE = ImmutableList.copyOf(Collections2.transform(
      ArticleJsonConstants.SYNDICATION_STATUSES,
      new Function<String, String>() {
        @Override
        public String apply(String input) {
          return input.toLowerCase();
        }
      }));

  /**
   * Serve the root page to a browser.
   */
  @RequestMapping(value = "/")
  public ModelAndView rootPage(ModelAndView model) throws IOException {
    model.addObject("stateParams", ArticleJsonConstants.PUBLICATION_STATE_NAMES);
    model.addObject("syndStatuses", SYNDICATION_STATUSES_LOWERCASE);
    model.addObject("version", getVersion());

    model.setViewName("root");
    return model;
  }

  private String getVersion() throws IOException {
    Properties properties = new Properties();
    Closer closer = Closer.create();
    try {
      InputStream is = closer.register(getClass().getResourceAsStream("/version.properties"));
      properties.load(is);
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
    return (String) properties.get("version");
  }
}
