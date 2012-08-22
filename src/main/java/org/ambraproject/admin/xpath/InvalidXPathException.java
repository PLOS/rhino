/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin.xpath;

import javax.xml.xpath.XPathExpressionException;

/**
 * Indicate that a constant, program-defined XPath expression could not be compiled.
 * <p/>
 * This represents cases where the error is expected to be caused only by programmer error, which means that an
 * unchecked exception is more appropriate. If so, wrap this class around the checked {@link XPathExpressionException}.
 * If the exception can be caused by user error, it is better to duck under or handle the {@link
 * XPathExpressionException}.
 */
public class InvalidXPathException extends RuntimeException {

  /**
   * @param expression the XPath expression that could not be compiled
   * @param exception  the object representing the expression compilation error
   */
  public InvalidXPathException(String expression, XPathExpressionException exception) {
    super(buildMessage(expression), exception);
  }

  private static String buildMessage(String expression) {
    return "Invalid XPath expression: " + expression;
  }

}
