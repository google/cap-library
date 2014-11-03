/*
 * Copyright (C) 2014 Google Inc.
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

package com.google.publicalerts.cap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.publicalerts.cap.Reason.Level;

import junit.framework.TestCase;

import java.util.Locale;

/**
 * Tests for {@link Reasons}.
 *
* @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class ReasonsTest extends TestCase {
  private final Reason errorReason1 =
      buildMockReason("1", MockReasonType.ERROR_TYPE);
  private final Reason errorReason2 =
      buildMockReason("2", MockReasonType.ERROR_TYPE);
  private final Reason warningReason1 =
      buildMockReason("3", MockReasonType.WARNING_TYPE);
  private final Reason warningReason2 =
      buildMockReason("4", MockReasonType.WARNING_TYPE);
  private final Reason recommendationReason1 =
      buildMockReason("5", MockReasonType.RECOMMENDATION_TYPE);
  private final Reason recommendationReason2 =
      buildMockReason("6", MockReasonType.RECOMMENDATION_TYPE);
  
  private final ImmutableList<Reason> allErrors =
      ImmutableList.of(errorReason1, errorReason2);
  private final ImmutableList<Reason> allWarnings =
      ImmutableList.of(warningReason1, warningReason2);
  private final ImmutableList<Reason> allRecommendations =
      ImmutableList.of(recommendationReason1, recommendationReason2);
  
  public ReasonsTest(String s) {
    super(s);
  }

  public void test_noLevelMissing() {
    Reasons.Builder builder = Reasons.newBuilder();
    
    builder.add(errorReason1);
    builder.add(errorReason2);
    builder.add(warningReason1);
    builder.add(warningReason2);
    builder.addAll(allRecommendations);    
    
    Reasons reasons = builder.build();
    
    assertEquals(
        ImmutableSet.builder()
            .addAll(allErrors)
            .addAll(allWarnings)
            .addAll(allRecommendations)
            .build(),
        ImmutableSet.copyOf(reasons));
    assertEquals(
        ImmutableSet.builder()
            .addAll(allErrors)
            .addAll(allRecommendations)
            .addAll(allWarnings)
            .build(),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.RECOMMENDATION)));
    assertTrue(reasons.containsWithLevelOrHigher(Level.RECOMMENDATION));
    assertEquals(
        ImmutableSet.builder()
            .addAll(allErrors)
            .addAll(allWarnings)
            .build(),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.WARNING)));
    assertTrue(reasons.containsWithLevelOrHigher(Level.WARNING));
    assertEquals(
        ImmutableSet.builder()
            .addAll(allErrors)
            .build(),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.ERROR)));
    assertTrue(reasons.containsWithLevelOrHigher(Level.ERROR));
  }
  
  public void test_someLevelsMissing() {
    Reasons.Builder builder = Reasons.newBuilder();
    
    builder.add(warningReason1);
    builder.add(warningReason2);
    builder.addAll(allRecommendations);    
    
    Reasons reasons = builder.build();
    
    assertEquals(
        ImmutableSet.builder()
            .addAll(allWarnings)
            .addAll(allRecommendations)
            .build(),
        ImmutableSet.copyOf(reasons));
    assertEquals(
        ImmutableSet.builder()
            .addAll(allWarnings)
            .addAll(allRecommendations)
            .build(),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.RECOMMENDATION)));
    assertTrue(reasons.containsWithLevelOrHigher(Level.RECOMMENDATION));
    assertEquals(
        ImmutableSet.copyOf(allWarnings),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.WARNING)));
    assertTrue(reasons.containsWithLevelOrHigher(Level.WARNING));
    assertEquals(
        ImmutableSet.of(),
        ImmutableSet.copyOf(
            reasons.getWithLevelOrHigher(Level.ERROR)));
    assertFalse(reasons.containsWithLevelOrHigher(Level.ERROR));
  }
  
  public void prefixWithXpath() {
    Reasons reasons = Reasons.of(warningReason1, warningReason2);

    assertEquals(
        ImmutableList.of(
            buildMockReason("/prefix3", MockReasonType.WARNING_TYPE),
            buildMockReason("/prefix4", MockReasonType.WARNING_TYPE)),
        ImmutableList.copyOf(reasons.prefixWithXpath("/prefix")));
  }
  
  private Reason buildMockReason(String uniqueString, MockReasonType type) {
    return new Reason(uniqueString, type);
  }
  
  private enum MockReasonType implements Reason.Type {
    ERROR_TYPE(Reason.Level.ERROR, "Error"),
    WARNING_TYPE(Reason.Level.WARNING, "Warning"),
    RECOMMENDATION_TYPE(Reason.Level.RECOMMENDATION, "Recommendation"),
    ;
    
    private final Reason.Level defaultLevel;
    private final String message;

    private MockReasonType(Reason.Level defaultLevel, String message) {
      this.defaultLevel = defaultLevel;
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }

    @Override
    public Reason.Level getDefaultLevel() {
      return defaultLevel;
    }
    
    @Override
    public String getSource() {
      return "Mock";
    }
  }
}
