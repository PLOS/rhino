package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.view.ArticleJsonConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
  public ModelAndView rootPage(ModelAndView model) {
    model.addObject("stateParams", ArticleJsonConstants.PUBLICATION_STATE_NAMES);
    model.addObject("syndStatuses", SYNDICATION_STATUSES_LOWERCASE);

    model.setViewName("root");
    return model;
  }

}
