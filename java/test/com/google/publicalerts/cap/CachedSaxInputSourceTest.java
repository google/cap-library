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

package com.google.publicalerts.cap;

import static com.google.publicalerts.cap.CachedSaxInputSource.CHUNK_SIZE;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

/**
 * Tests for {@link CachedSaxInputSource}.
 *
 * @author pcoakley@google.com (Phil Coakley)
 */
public class CachedSaxInputSourceTest extends TestCase {
  public CachedSaxInputSourceTest(String s) {
    super(s);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testByteSource() throws Exception {
    int inputLength = (int)(CHUNK_SIZE * 2.5);
    byte[] bytes = repeat("z", inputLength).getBytes("UTF-8");
    InputStream is = new ByteArrayInputStream(bytes);

    CachedSaxInputSource source = new CachedSaxInputSource(new InputSource(is));
    assertNull("Source.getCharacterStream", source.getCharacterStream());
    assertNotNull("Source.getByteStream", source.getByteStream());

    // Completely consume the source.
    consume(source.getByteStream(), inputLength);

    // Reset source back to the beginning.
    source.reset();

    // We should be able to completely consume the source again.
    consume(source.getByteStream(), inputLength);

    // Close the source.  This should be a no-op, with no effect on our ability
    // to re-read.
    source.getByteStream().close();
    source.reset();
    consume(source.getByteStream(), inputLength);
  }

  public void testReaderSource() throws Exception {
    int inputLength = (int)(CHUNK_SIZE * 2.5);
    StringReader input = new StringReader(repeat("z", inputLength));
    doTestCharacterSource(new CachedSaxInputSource(input), inputLength);
  }

  public void testStringSource() throws Exception {
    int inputLength = (int)(CHUNK_SIZE * 2.5);
    String input = repeat("z", inputLength);
    doTestCharacterSource(new CachedSaxInputSource(input), inputLength);
  }

  private void doTestCharacterSource(CachedSaxInputSource source, int inputLength)
      throws Exception {
    assertNotNull("Source.getCharacterStream", source.getCharacterStream());
    assertNull("Source.getByteStream", source.getByteStream());

    // Completely consume the source.
    consume(source.getCharacterStream(), inputLength);

    // Reset source back to the beginning.
    source.reset();

    // We should be able to completely consume the source again.
    consume(source.getCharacterStream(), inputLength);

    // Close the source.  This should be a no-op, with no effect on our ability
    // to re-read.
    source.getCharacterStream().close();
    source.reset();
    consume(source.getCharacterStream(), inputLength);
  }

  private void consume(Reader charStream, int expectedLength) throws IOException {
    CharBuffer sink = CharBuffer.allocate(expectedLength);
    int charsRead = charStream.read(sink);
    assertEquals("Expected sink size == input size", expectedLength, charsRead);
    assertEquals("Expected end of stream", -1, charStream.read());
  }

  private void consume(InputStream byteStream, int expectedLength) throws IOException {
    int bytesRead = 0;
    while (byteStream.read() > 0) {
      bytesRead++;
    }
    assertEquals("Expected sink size == input size", expectedLength, bytesRead);
    assertEquals("Expected end of stream", -1, byteStream.read());
  }

  private static String repeat(String string, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(string);
    }
    return sb.toString();
  }
}
