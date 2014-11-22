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

import com.google.publicalerts.cap.edxl.io.ConverterForEdxlde10Test;
import com.google.publicalerts.cap.edxl.io.Edxlde10ParserTest;
import com.google.publicalerts.cap.feed.CapFeedParserTest;
import com.google.publicalerts.cap.feed.CapFeedValidatorTest;
import com.google.publicalerts.cap.profile.GoogleProfileTest;
import com.google.publicalerts.cap.profile.au.AustralianProfileTest;
import com.google.publicalerts.cap.profile.ca.CanadianProfileTest;
import com.google.publicalerts.cap.profile.us.Ipaws1ProfileTest;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * All tests.
 *
 * To run, use
 * ant test
 *
 * To run an individual test, use
 * ant test -Dtest=EndToEndTest
 *
 * To run an indivisual test case, use
 * ant test -Dtest=EndToEndTest -Dtests=testEndToEnd
 *
* @author shakusa@google.com (Steve Hakusa)
 */
public class AllTests {

  private static final String TEST = "test";
  private static final String TEST_ANT_PROPERTY = "${test}";
  private static final String TEST_CASES = "tests";
  private static final String TEST_CASES_ANT_PROPERTY = "${tests}";
  private static final String DELIMITER = ",";

  public static TestSuite allTests() {
    // TODO(shakusa) Automate this, recursively search for test classes
    TestSuite suite = new TestSuite();

    suite.addTestSuite(CachedSaxInputSourceTest.class);
    suite.addTestSuite(CapJsonBuilderTest.class);
    suite.addTestSuite(CapXmlParserTest.class);
    suite.addTestSuite(CapXmlBuilderTest.class);
    suite.addTestSuite(CapUtilTest.class);
    suite.addTestSuite(CapValidatorTest.class);
    suite.addTestSuite(XmlSignAndValidateTest.class);
    suite.addTestSuite(EndToEndTest.class);
    suite.addTestSuite(ReasonTest.class);
    suite.addTestSuite(ReasonsTest.class);
    suite.addTestSuite(XPathTest.class);
    suite.addTestSuite(XercesCapExceptionMapperTest.class);

    suite.addTestSuite(CapFeedParserTest.class);
    suite.addTestSuite(CapFeedValidatorTest.class);

    suite.addTestSuite(Edxlde10ParserTest.class);
    suite.addTestSuite(ConverterForEdxlde10Test.class);

    suite.addTestSuite(AustralianProfileTest.class);
    suite.addTestSuite(CanadianProfileTest.class);
    suite.addTestSuite(GoogleProfileTest.class);
    suite.addTestSuite(Ipaws1ProfileTest.class);

    return suite;
  }

  @SuppressWarnings("unchecked")
  public static TestSuite suite() {
    Class<?> testClass = null;
    if (hasTest()) {
      try {
        testClass = Class.forName(
            "com.google.publicalerts.cap." + System.getProperty(TEST));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    if (hasTestCases()) {
      return getSuite(testClass);
    } else if (testClass != null) {
      return new TestSuite(testClass);
    }

    return allTests();
  }

  private static boolean hasTest() {
    return System.getProperty(TEST) != null &&
        !System.getProperty(TEST).equals(TEST_ANT_PROPERTY);
  }

  private static boolean hasTestCases() {
    return System.getProperty(TEST_CASES) != null &&
        !System.getProperty(TEST_CASES).equals(TEST_CASES_ANT_PROPERTY);
  }

  // TODO(shakusa) Annoyingly, for this to work, all TestCase's have to have a
  // constructor that takes String. Use reflection to avoid that.
  private static TestSuite getSuite(Class<?> testClass) {
    if (!TestCase.class.isAssignableFrom(testClass)) {
      throw new IllegalArgumentException(
          testClass + " must be a subclass of TestCase");
    }

    TestSuite suite = new TestSuite();
    try {
      Constructor<?> constructor = testClass.getConstructor(
          new Class[] {String.class});
      List<String> testCaseNames = getTestCaseNames();
      for (String testCaseName : testCaseNames) {
        suite.addTest((TestCase) constructor.newInstance(testCaseName));
      }
    } catch (Exception e) {
      throw new RuntimeException(
          testClass.getName() + " doesn't have the proper constructor");
    }
    return suite;
  }

  private static List<String> getTestCaseNames() {
    String testCases = System.getProperty(TEST_CASES);
    List<String> ret = new ArrayList<String>();
    for (String testCase : testCases.split(DELIMITER)) {
      ret.add(testCase.trim());
    }
    return ret;
  }
}
