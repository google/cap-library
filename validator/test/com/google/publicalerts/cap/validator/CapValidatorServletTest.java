/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.publicalerts.cap.validator;

import static org.mockito.Mockito.mock;

import com.google.publicalerts.cap.validator.CapValidatorServlet.CapExample;

import junit.framework.TestCase;

import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Tests for {@link CapValidatorServlet}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorServletTest extends TestCase {
  private CapValidator capValidator;
  private ServletFileUpload upload;
  private CapValidatorServlet servlet;

  public CapValidatorServletTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.capValidator = mock(CapValidator.class);
    this.upload = mock(ServletFileUpload.class);
    this.servlet = new CapValidatorServlet(capValidator, upload);
  }

  public void testLoadExample() throws Exception {
    String earthquake = servlet.loadExample("earthquake-atom-feed");
    assertNotNull(earthquake);
  }
  
  public void testCapExampleGetLabel() {
    assertEquals(
        "earthquake-atom-feed", CapExample.EARTHQUAKE_ATOM_FEED.getLabel());
  }

  public void testCapExampleFromLabel() {
    assertEquals(CapExample.EARTHQUAKE_ATOM_FEED,
        CapExample.fromLabel("earthquake-atom-feed"));
  }
  
  public void testCapExampleFromLabel_unknownLabel() {
    try {
      CapExample.fromLabel("foo-bar");
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  // TODO(shakusa) Test loading from file
}
