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
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.Reason.Level;
import com.google.publicalerts.cap.testing.TestResources;
import com.google.publicalerts.cap.profile.CapProfile;
import com.google.publicalerts.cap.profile.us.Ipaws1Profile;

/**
 * Tests for {@link CapValidator}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorTest extends TestCase {
  private static final Set<CapProfile> NO_PROFILES = ImmutableSet.of();

  private TestCapValidator validator;

  public CapValidatorTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.validator = new TestCapValidator("earthquake.cap");
  }

  public void testNotXml() {
    String xml = "invalid";
    
    assertByLineValidationMessageMap(
        validator.validate(xml, NO_PROFILES),
        ImmutableListMultimap.of(1, Level.ERROR));
  }

  public void testUnsupportedXml() {
    String xml =
        "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><test></test>";
    
    // Expected error on line 1
    assertByLineValidationMessageMap(
        validator.validate(xml, NO_PROFILES),
        ImmutableListMultimap.of(1, Level.ERROR));
  }

  public void testInvalidCapXml() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    cap = cap.replaceAll("identifier", "invalid");
    
    // Expect an error on line 4 (where <identifier> was in the alert)
    assertByLineValidationMessageMap(
        validator.validate(cap, NO_PROFILES),
        ImmutableListMultimap.of(4, Level.ERROR));
  }

  public void testInvalidFeedXml() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    cap = cap.replaceAll("author", "authors");
    
    // Expect an error on line 7 (where <author> was in the alert)    
    assertByLineValidationMessageMap(
        validator.validate(cap, NO_PROFILES),
        ImmutableListMultimap.of(7, Level.ERROR));
  }

  public void testRssFeed() throws Exception {
    String cap = TestResources.load("ny_index.rss");
    ValidationResult result = validator.validate(cap, NO_PROFILES);
    
    // Expect no errors
    assertTrue(result.getByLineValidationMessages().isEmpty());
    assertEquals(2, result.getValidAlerts().size());
  }

  public void testEdxlde() throws Exception {
    String cap = TestResources.load("bushfire_valid.edxlde");
    ValidationResult result = validator.validate(cap, NO_PROFILES);
    
    ImmutableListMultimap<Integer, Level> expected =
        ImmutableListMultimap.<Integer, Level>builder()
            .put(39, Level.WARNING)  // HTML entity
            .put(115, Level.WARNING) // HTML entity
            .put(189, Level.WARNING) // HTML entity
            .put(263, Level.WARNING) // HTML entity
            .put(337, Level.WARNING) // HTML entity
            .put(411, Level.WARNING) // HTML entity
            .put(485, Level.WARNING) // HTML entity
            .build();
    
    assertEquals(7, result.getValidAlerts().size());
    assertByLineValidationMessageMap(result, expected);
  }
  
  public void testEdxldeError() throws Exception {
    String cap = TestResources.load("bushfire_invalid.edxlde");
    ValidationResult result = validator.validate(cap, NO_PROFILES);
    
    ImmutableListMultimap<Integer, Level> expected =
        ImmutableListMultimap.<Integer, Level>builder()
            .put(39, Level.WARNING)  // HTML entity
            .put(94, Level.ERROR)
            .put(115, Level.ERROR)
            .put(125, Level.WARNING) // HTML entity
            .put(199, Level.WARNING) // HTML entity
            .put(273, Level.WARNING) // HTML entity
            .put(347, Level.WARNING) // HTML entity
            .put(421, Level.WARNING) // HTML entity
            .build();
    
    assertEquals(7, result.getValidAlerts().size());
    assertByLineValidationMessageMap(result, expected);
  }
  
  public void testThinAtomFeed() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    ValidationResult result = validator.validate(cap, NO_PROFILES);
    
    // Expect no errors
    assertTrue(result.getByLineValidationMessages().isEmpty());
    assertEquals(1, result.getValidAlerts().size());
  }

  public void testThinAtomFeedCapError() throws Exception {
    validator.setFeed("invalid.cap");
    String cap = TestResources.load("earthquake_index.atom");

    // Expect an error at line 10, where the <link> appears
    assertByLineValidationMessageMap(
        validator.validate(cap, NO_PROFILES),
        ImmutableListMultimap.of(10, Level.ERROR));
  }

  public void testFatAtomFeed() throws Exception {
    String cap = TestResources.load("amber.atom");
    ValidationResult result = validator.validate(cap, NO_PROFILES);

    assertEquals(1, result.getValidAlerts().size());
    assertByLineValidationMessageMap(result, ImmutableListMultimap.of(34, Level.WARNING));
  }

  public void testFatAtomFeedNoEntries() throws Exception {
    String feed = TestResources.load("no_entries.atom");

    // Expect an error on the first line
    assertByLineValidationMessageMap(
        validator.validate(feed, NO_PROFILES),
        ImmutableListMultimap.of(1, Level.ERROR));
  }
  
  public void testCap() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    ValidationResult result = validator.validate(cap, NO_PROFILES);
    
    // Expect no errors
    assertTrue(result.getByLineValidationMessages().isEmpty());
    assertEquals(1, result.getValidAlerts().size());
  }

  public void testCapProfile() throws Exception {
    String cap = TestResources.load("earthquake.cap");
    ValidationResult result = validator.validate(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));
    
    // Expect no errors, 2 recommendations from the profile
    assertByLineValidationMessageMap(
        result,
        ImmutableListMultimap.of(
            11, Level.RECOMMENDATION,
            75, Level.RECOMMENDATION));
  }

  public void testFatAtomFeedCapProfile() throws Exception {
    String cap = TestResources.load("amber.atom");
    ValidationResult result = validator.validate(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));
    
    // Expect 2 errors, 2 recommendations
    assertByLineValidationMessageMap(
        result,
        ImmutableListMultimap.of(
            16, Level.ERROR,
            24, Level.ERROR,
            24, Level.RECOMMENDATION,
            34, Level.WARNING,
            41, Level.RECOMMENDATION));
  }

  public void testThinFeedWithCapProfile() throws Exception {
    String cap = TestResources.load("earthquake_index.atom");
    ValidationResult result = validator.validate(cap,
        Sets.<CapProfile>newHashSet(new Ipaws1Profile()));

    // Expect no errors, one recommendation entry at the link line
    assertByLineValidationMessageMap(
        result,
        ImmutableListMultimap.of(
            10, Level.RECOMMENDATION,
            10, Level.RECOMMENDATION));
  }

  private void assertByLineValidationMessageMap(
      ValidationResult actualResult, ListMultimap<Integer, Level> expected) {
    ListMultimap<Integer, Level> actualMap = LinkedListMultimap.create();
    
    for (Map.Entry<Integer, ValidationMessage> entry
        : actualResult.getByLineValidationMessages().entries()) {
      actualMap.put(entry.getKey(), entry.getValue().getLevel());
    }
    
    for (Integer key : actualMap.keySet()) {
      assertEquals(actualMap.toString(),
          ImmutableMultiset.copyOf(expected.get(key)),
          ImmutableMultiset.copyOf(actualMap.get(key)));
    }
    
    assertEquals(expected.size(), actualMap.size());
  }
  
  private static class TestCapValidator extends CapValidator {
    private static final long serialVersionUID = 1L;

    private String feed;

    public TestCapValidator(String feed) {
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
