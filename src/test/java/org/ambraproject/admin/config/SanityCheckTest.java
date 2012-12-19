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

package org.ambraproject.admin.config;

import org.ambraproject.admin.BaseAdminTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;


/**
 * Meta-test to check whether the Spring/TestNG testing harness is configured correctly.
 */
public class SanityCheckTest extends BaseAdminTest {

  @Autowired
  private Object sampleBean;

  /**
   * Check that beans configured in {@link TestConfiguration} are accessible in the {@code applicationContext} provided
   * by the framework's testing superclass.
   */
  @Test
  public void testApplicationContext() {
    logger.debug("testApplicationContext");
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean("sampleBean"));
  }

  /**
   * Check that beans configured in {@link TestConfiguration} are autowired correctly.
   */
  @Test
  public void testAutowiring() {
    logger.debug("testAutowiring");
    assertNotNull(sampleBean);
  }

}
