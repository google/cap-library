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

import java.io.IOException;
import java.util.Set;

import junit.framework.TestCase;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.feed.TestResources;
import com.google.publicalerts.cap.profile.CapProfile;
import com.google.publicalerts.cap.profile.Ipaws1Profile;

/**
 * Tests for {@link CapValidatorServlet}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorServletTest extends TestCase {
  private static final Set<CapProfile> NO_PROFILES = ImmutableSet.of();

  private TestCapValidatorServlet servlet;

  public CapValidatorServletTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.servlet = new TestCapValidatorServlet("earthquake.cap");
  }

  public void testNotXml() {
    ValidationResult result = servlet.handleInput("invalid", NO_PROFILES);
    assertFalse(result.getByLineErrorMap().isEmpty());
    assertFalse(result.getByLineErrorMap().get(1).isEmpty());
  }

  public void testUnsupportedXml() {
    ValidationResult result = servlet.handleInput(
        "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><test></test>",
        NO_PROFILES);
    // expect an error on line 1
    assertFalse(result.getByLineErrorMap().isEmpty());
    assertFalse(result.getByLineErrorMap().get(1).isEmpty());
  }

  public void testInvalidCapXml() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    cap = cap.replaceAll("identifier", "invalid");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect an error on line 4 (where <identifier> was in the alert)
    assertFalse(result.getByLineErrorMap().isEmpty());
    assertFalse(result.getByLineErrorMap().get(4).isEmpty());
  }

  public void testInvalidFeedXml() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    cap = cap.replaceAll("author", "authors");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect an error on line 7 (where <author> was in the alert)
    assertFalse(result.getByLineErrorMap().isEmpty());
    assertFalse(result.getByLineErrorMap().get(7).isEmpty());
  }

  public void testRssFeed() throws Exception {
    String cap = TestResources.load("ny_index.rss");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect no errors
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertTrue(result.getByLineRecommendationMap().isEmpty());
    assertEquals(2, result.getValidAlerts().size());
  }

  public void testThinAtomFeed() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect no errors
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertTrue(result.getByLineRecommendationMap().isEmpty());
    assertEquals(1, result.getValidAlerts().size());
  }

  public void testThinAtomFeedCapError() throws Exception {
    servlet.setFeed("invalid.cap");
    String cap = TestResources.load("earthquake_index.atom");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect an error at line 10, where the <link> appears
    assertFalse(result.getByLineErrorMap().isEmpty());
    assertFalse(result.getByLineErrorMap().get(10).isEmpty());
  }

  public void testFatAtomFeed() throws Exception {
    String cap = TestResources.load("amber.atom");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect no errors
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertTrue(result.getByLineRecommendationMap().isEmpty());
    assertEquals(1, result.getValidAlerts().size());
  }

  public void testCap() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    ValidationResult result = servlet.handleInput(cap, NO_PROFILES);
    // Expect no errors
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertTrue(result.getByLineRecommendationMap().isEmpty());
    assertEquals(1, result.getValidAlerts().size());
  }

  public void testCapProfile() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    ValidationResult result = servlet.handleInput(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));
    // Expect no errors, 2 recommendations from the profile
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertEquals(4, result.getByLineRecommendationMap().size());
    assertFalse(result.getByLineRecommendationMap().get(11).isEmpty());
    assertFalse(result.getByLineRecommendationMap().get(72).isEmpty());
  }

  public void testFatAtomFeedCapProfile() throws Exception {
    String cap = TestResources.load("amber.atom");
    ValidationResult result = servlet.handleInput(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));
    // Expect 2 errors, 2 recommendations
    assertEquals(2, result.getByLineErrorMap().size());
    assertFalse(result.getByLineErrorMap().get(16).isEmpty());
    assertFalse(result.getByLineErrorMap().get(24).isEmpty());
    assertEquals(2, result.getByLineRecommendationMap().size());
    assertFalse(result.getByLineRecommendationMap().get(24).isEmpty());
    assertFalse(result.getByLineRecommendationMap().get(41).isEmpty());
  }

  public void testThinFeedWithCapProfile() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    ValidationResult result = servlet.handleInput(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));
    // Expect no errors, ! aggregate recommendation entry at the link line
    assertTrue(result.getByLineErrorMap().isEmpty());
    assertEquals(1, result.getByLineRecommendationMap().size());
    assertFalse(result.getByLineRecommendationMap().get(10).isEmpty());
  }

  private static class TestCapValidatorServlet extends CapValidatorServlet {
    private static final long serialVersionUID = 1L;

    private String feed;

    public TestCapValidatorServlet(String feed) {
      this.feed = feed;
    }

    public void setFeed(String feed) {
      this.feed = feed;
    }

    @Override
    String loadUrl(String capUrl) {
      try {
        return TestResources.load(feed);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
