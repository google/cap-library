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

/**
 * This package contains the core classes that encode the CAP protocol as
 * well as classes that can parse and write CAP messages from a variety of
 * wire formats.
 * <p>
 * The main data structures are auto-generated from a Google protocol
 * buffer implementation of the CAP spec in
 * <a href="http://code.google.com/p/cap-library/source/browse/proto/cap.proto">cap.proto</a>.
 * Protocol buffers
 * are Google's language-neutral, platform-neutral, extensible mechanism for
 * serializing structured data - think XML, but smaller, faster, and simpler.
 * <p>
 * The generated classes offer a clean API for creating and manipulating
 * alert objects. The alert data structures are immutable; they provide only
 * getters. New alerts are constructed via Builder classes. See
 * <a href="http://code.google.com/p/cap-library/source/browse/java/test/com/google/publicalerts/cap/CapTestUtil.java">CapTestUtil.java</a>
 * for an example.
 * <p>
 * <a href="http://code.google.com/p/cap-library/source/browse/java/test/com/google/publicalerts/cap/EndToEndTest.java">EndToEndTest.java</a>
 * provides a good overview of how to get started using the library.
 * <p>
 * To learn more about Google protocol buffers, see
 * <a href="http://code.google.com/apis/protocolbuffers/">http://code.google.com/apis/protocolbuffers</a>
 * and
 * <a href="http://code.google.com/apis/protocolbuffers/docs/reference/java-generated.html">http://code.google.com/apis/protocolbuffers/docs/reference/java-generated.html</a>
 */
package com.google.publicalerts.cap;
