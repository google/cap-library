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

import com.google.publicalerts.cap.AlertOrBuilder;
import com.google.publicalerts.cap.CapException;

import java.util.List;

/**
 * Specifies a set of extra constraints on the CAP format.
 * The constraints are usually country-specific.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public interface CapProfile {

  /**
   * Returns the name of the profile, suitable for display.
   *
   * @return the name of the profile
   */
  String getName();

  /**
   * Returns a code unique to the profile.  Ideally, it will
   * include a version number.
   *
   * @return a unique code identifying the profile
   */
  String getCode();

  /**
   * Returns the canonical URL of the official document describing
   * the profile.
   *
   * @return the documentation url
   */
  String getDocumentationUrl();

  /**
   * Checks the given alert against the profile and returns a list
   * of errors explaining how the alert does not match the profile.
   * The profile should assume that the given alert is valid CAP and
   * check only the extra constraints it specifies.
   *
   * @param alert the alert to check
   * @return errors errors found during the check, an empty list
   * if there are none
   */
  List<CapException.Reason> checkForErrors(AlertOrBuilder alert);

  /**
   * Checks the given alert against suggestions and best-practices
   * recommended by the profile. An alert not adhering to these
   * recommendations would still be accepted by whatever system
   * that claims to require alerts adhering to the profile, but
   * adopting the recommendations allows for more full compatibility
   * and perhaps extra features.
   *
   * @param alert the alert to check
   * @return recommendations found during the check, an empty list if
   * there are none
   */
  List<CapException.Reason> checkForRecommendations(AlertOrBuilder alert);
}
