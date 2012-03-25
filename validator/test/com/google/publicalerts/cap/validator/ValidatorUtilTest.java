/*
 * Copyright (C) 2012 Google Inc.
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

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import com.google.publicalerts.cap.profile.CapProfile;
import com.google.publicalerts.cap.profile.CapProfiles;

/**
 * Tests for {@link ValidatorUtil}.
 *
 * @author Steven Hakusa (shakusa@google.com)
 */
public class ValidatorUtilTest extends TestCase {
  public void testGetProfilesJsp() {
    CapProfile profile = CapProfiles.getProfiles().get(0);
    Set<CapProfile> profiles = Sets.newHashSet(profile);
    List<String[]> profilesJsp = ValidatorUtil.getProfilesJsp(profiles);

    assertEquals(2, profilesJsp.size());
    assertEquals(profile.getCode(), profilesJsp.get(0)[0]);
    assertEquals("checked", profilesJsp.get(0)[1]);
    assertEquals("", profilesJsp.get(1)[1]);
  }

  public void testParseProfiles() {
    assertTrue(ValidatorUtil.parseProfiles(null).isEmpty());
    assertTrue(ValidatorUtil.parseProfiles("").isEmpty());

    assertEquals(1, ValidatorUtil.parseProfiles("IPAWSv1.0").size());
    assertEquals(2,
        ValidatorUtil.parseProfiles("IPAWSv1.0,profile:CAP-CP:1.0").size());
  }

  public void testToCallbackUrl() {
    String url = ValidatorUtil.toCallbackUrl("http://localhost",
        "http://topic.com/feed?a=b&c=d", "test+foo@example.com");

    assertEquals("http://localhost" + 
        "?topic=http%3A%2F%2Ftopic.com%2Ffeed%3Fa%3Db%26c%3Dd" +
        "&email=test%2Bfoo%40example.com", url);
  }
}
