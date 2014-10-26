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

package com.google.publicalerts.cap.profile;

import com.google.common.collect.ImmutableList;
import com.google.publicalerts.cap.profile.au.AustralianProfile;
import com.google.publicalerts.cap.profile.ca.CanadianProfile;
import com.google.publicalerts.cap.profile.us.Ipaws1Profile;

import java.util.List;

/**
 * Repository of all {@link CapProfile}s.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapProfiles {

  private static final List<CapProfile> PROFILES =
      ImmutableList.<CapProfile>builder()
          .add(new Ipaws1Profile())
          .add(new CanadianProfile())
          .add(new AustralianProfile())
          .add(new GoogleProfile())
          .build();

  public static List<CapProfile> getProfiles() {
    return PROFILES;
  }

  private CapProfiles() {}
}
