// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.publicalerts.cap;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.google.publicalerts.cap.feed.CapFeedParserTest;
import com.google.publicalerts.cap.feed.CapFeedValidatorTest;

/**
 * All tests.
 *
 * To run, use
 * ant test
 *
 * To run an individual test, use
 * ant test -Dtest=CapXmlParserTest
 *
 * To run an indivisual test case, use
 * ant test -Dtest=CapXmlParserTest -Dtests=testParseAlert
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

    suite.addTestSuite(CapXmlParserTest.class);
    suite.addTestSuite(CapUtilTest.class);
    suite.addTestSuite(CapValidatorTest.class);

    suite.addTestSuite(CapFeedParserTest.class);
    suite.addTestSuite(CapFeedValidatorTest.class);

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
