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

package com.google.publicalerts.cap;

/**
 * An exception for when data is not in the CAP format.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class NotCapException extends RuntimeException {
  private static final long serialVersionUID = 5192142568310923665L;

  public NotCapException() {
  }

  public NotCapException(String message) {
    super(message);
  }

  public NotCapException(String message, Throwable cause) {
    super(message, cause);
  }
}
