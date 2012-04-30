// Copyright 2012 Google Inc. All Rights Reserved.
package com.google.publicalerts.cap.profile;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Tests for {@link CsvFileIterator}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CsvFileIteratorTest extends TestCase {

  private CsvFileIterator itr;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    itr = new CsvFileIterator(new StringInputStream("1,\"2,3\"\"\",4\"\n5,,"));
  }

  public void testBasic() {
    assertTrue(itr.hasNext());
    assertArraysEqual(new String[] {"1", "2,3\"", "4\""}, itr.next().getCells());
    assertTrue(itr.hasNext());
    assertArraysEqual(new String[] {"5", "", ""}, itr.next().getCells());
    assertFalse(itr.hasNext());
    try {
      itr.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
      // expected
    }
  }

  public void testCsvRow() {
    CsvFileIterator.CsvRow row = itr.next();
    assertEquals("1", row.getAt(0, null));
    assertEquals("2,3\"", row.getAt(1, null));
    assertEquals("default", row.getAt(-1, "default"));
    assertEquals("default", row.getAt(row.getCells().length, "default"));
  }

  public void testUnescape() throws Exception {
    assertEquals("", itr.unescape(""));
    assertEquals("a", itr.unescape("a"));
    assertEquals("\"", itr.unescape("\""));
    assertEquals("a", itr.unescape("\"a\""));
    assertEquals("\"a\"", itr.unescape("\"\"\"a\"\"\""));
    assertEquals("\"", itr.unescape("\"\"\""));
    assertEquals("braun \"coffee makers\"",
                 itr.unescape("\"braun \"\"coffee makers\"\"\""));
    assertEquals("abc", itr.unescape("\"abc\""));
    assertEquals("abc\"", itr.unescape("\"abc\"\"\""));
    assertEquals("abc\"", itr.unescape("\"abc\"\""));
    assertEquals("ab\"c", itr.unescape("ab\"c"));
    assertEquals("abc", itr.unescape("\"ab\"c"));
    assertEquals("abc", itr.unescape("\"abc\""));
    assertEquals("ab\"c", itr.unescape("\"a\"b\"c"));
    assertEquals("ab\"c\"", itr.unescape("\"a\"b\"c\""));
    assertEquals("a\"\"bc", itr.unescape("a\"\"bc"));
    assertEquals("abc\"", itr.unescape("\"a\"bc\""));
    assertEquals("a\nbc", itr.unescape("\"a\nb\"c"));
    assertEquals("a\nbc\"", itr.unescape("\"a\nb\"c\""));
  }

  public void testSplitRow() {
    String[] s1 = {"foo", "bar", "baz"};
    assertArraysEqual(s1, itr.splitRow("foo,bar,baz"));
    String[] s2 = {"foo,bar", "baz\""};
    assertArraysEqual(s2, itr.splitRow("\"foo,bar\",baz\""));
    String[] s3 = {"hello", "", ""};
    assertArraysEqual(s3, itr.splitRow("hello,,"));
  }

  private void assertArraysEqual(String[] a, String[] b) {
    assertEquals(Arrays.toString(a), Arrays.toString(b));
  }

  private static class StringInputStream extends InputStream {

    private final byte[] bytes;
    private int i = 0;

    public StringInputStream(String s) {
      bytes = s.getBytes();
    }

    @Override
    public int read() throws IOException {
      return i >= bytes.length ? -1 : bytes[i++];
    }
  }
}
