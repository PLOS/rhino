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

package org.ambraproject.rhino.content.xml;

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
