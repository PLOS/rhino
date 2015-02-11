/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service.classifier;

import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Map;

/**
 * @author Alex Kudlick Date: 7/3/12
 */
public interface ArticleClassifier {

  /**
   * Classify an article from its xml.
   *
   * @param articleXml the article xml
   * @return a map of categories to which the article belongs. Each entry should use <code>/</code>s to delimit subject
   * hierarchy.  Categories are returned in descending order of the strength of the match paired with the strength
   * value
   */
  public Map<String, Integer> classifyArticle(Document articleXml) throws IOException;

}
