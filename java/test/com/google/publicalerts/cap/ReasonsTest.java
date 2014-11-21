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

import static com.google.common.truth.Truth.assertThat;
import static com.google.publicalerts.cap.Reason.Level.ERROR;
import static com.google.publicalerts.cap.Reason.Level.RECOMMENDATION;
import static com.google.publicalerts.cap.Reason.Level.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.Reason.Level;

import junit.framework.TestCase;

import java.util.Locale;

/**
 * Tests for {@link Reasons}.
 *
* @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class ReasonsTest extends TestCase {
  private final Reason errorReason1 = new Reason("/e[1]", MockReasonType.ERROR_TYPE);
  private final Reason errorReason2 = new Reason("/e[2]", MockReasonType.ERROR_TYPE);
  private final Reason warningReason1 = new Reason("/w[1]", MockReasonType.WARNING_TYPE);
  private final Reason warningReason2 = new Reason("/w[2]", MockReasonType.WARNING_TYPE);
  private final Reason recommendationReason1 =
      new Reason("/r[1]", MockReasonType.RECOMMENDATION_TYPE);
  private final Reason recommendationReason2 =
      new Reason("/r[2]", MockReasonType.RECOMMENDATION_TYPE);
  
  private final ImmutableSet<Reason> allErrors =
      ImmutableSet.of(errorReason1, errorReason2);
  private final ImmutableSet<Reason> allWarnings =
      ImmutableSet.of(warningReason1, warningReason2);
  private final ImmutableSet<Reason> allRecommendations =
      ImmutableSet.of(recommendationReason1, recommendationReason2);
  
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
    
    assertThat(reasons).containsExactlyElementsIn(
        Sets.union(allErrors, Sets.union(allWarnings, allRecommendations)));
    
    assertThat(reasons.getWithLevelOrHigher(RECOMMENDATION)).containsExactlyElementsIn(
        Sets.union(allErrors, Sets.union(allWarnings, allRecommendations)));
    assertThat(reasons.containsWithLevelOrHigher(RECOMMENDATION)).isTrue();

    assertThat(reasons.getWithLevelOrHigher(WARNING))
        .containsExactlyElementsIn(Sets.union(allErrors, allWarnings));
    assertThat(reasons.containsWithLevelOrHigher(WARNING)).isTrue();
    
    assertThat(reasons.getWithLevelOrHigher(ERROR)).containsExactlyElementsIn(allErrors);
    assertThat(reasons.containsWithLevelOrHigher(ERROR)).isTrue();
  }
  
  public void test_someLevelsMissing() {
    Reasons.Builder builder = Reasons.newBuilder();
    
    builder.add(warningReason1);
    builder.add(warningReason2);
    builder.addAll(allRecommendations);    
    
    Reasons reasons = builder.build();
    
    assertThat(reasons).containsExactlyElementsIn(Sets.union(allWarnings, allRecommendations));
    
    assertThat(reasons.getWithLevelOrHigher(RECOMMENDATION))
        .containsExactlyElementsIn(Sets.union(allWarnings, allRecommendations));
    assertThat(reasons.containsWithLevelOrHigher(RECOMMENDATION)).isTrue();

    assertThat(reasons.getWithLevelOrHigher(WARNING)).containsExactlyElementsIn(allWarnings);
    assertThat(reasons.containsWithLevelOrHigher(WARNING)).isTrue();
    
    assertThat(reasons.getWithLevelOrHigher(ERROR)).isEmpty();
    assertThat(reasons.containsWithLevelOrHigher(ERROR)).isFalse();
  }
  
  public void testPrefixWithXpath() {
    Reasons reasons = Reasons.of(warningReason1, warningReason2);

    assertThat(reasons.prefixWithXpath("/prefix[1]")).containsExactly(
        new Reason("/prefix[1]/w[1]", MockReasonType.WARNING_TYPE),
        new Reason("/prefix[1]/w[2]", MockReasonType.WARNING_TYPE));
  }

  private enum MockReasonType implements Reason.Type {
    ERROR_TYPE(ERROR, "Error"),
    WARNING_TYPE(WARNING, "Warning"),
    RECOMMENDATION_TYPE(RECOMMENDATION, "Recommendation"),
    ;
    
    private final Level defaultLevel;
    private final String message;

    private MockReasonType(Level defaultLevel, String message) {
      this.defaultLevel = defaultLevel;
      this.message = message;
    }

    @Override
    public String getMessage(Locale locale) {
      return message;
    }

    @Override
    public Level getDefaultLevel() {
      return defaultLevel;
    }
    
    @Override
    public String getSource() {
      return "Mock";
    }
  }
}
