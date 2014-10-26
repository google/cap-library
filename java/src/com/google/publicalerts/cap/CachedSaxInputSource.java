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

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * An implementation of {@link InputSource} that allows source content to be
 * read multiple times.  This class completely consumes and closes all
 * underlying data streams.
 *
 * <p>The driving motivation for this class is our need to read a document's
 * CAP Alert XMLNS prior to actually parsing the document.
 *
 * <p>Call {@link #reset} in-between uses.  There is no need to close the
 * encapsulated streams: since they are already cached, they will simply be
 * garbage collected when they're no longer needed.
 *
 * @author pcoakley@google.com (Phil Coakley)
 */
public class CachedSaxInputSource extends InputSource {
  /**
   * Size of intermediate buffer used to copy data from streams to caches.
   */
  static final int CHUNK_SIZE = 8192;

  /**
   * Creates an InputSource that provides data from the given String.
   * The given String is simply cached as-is, making this the most efficient
   * constructor.
   */
  CachedSaxInputSource(String string) {
    super(new CachedCharStream(string));
  }

  /**
   * Creates an InputSource that provides data from the given Reader.
   * The Reader is completely consumed, cached, and closed prior to the
   * return of this method.
   *
   * @throws SAXParseException if there is any error consuming reader
   */
  CachedSaxInputSource(Reader reader) throws SAXParseException {
    super(CachedCharStream.from(reader));
  }

  /**
   * Wraps the given InputSource to provide a cached view of the data within.
   * All contained streams are completely consumed, cached and closed prior
   * to the return of this method.
   *
   * @throws SAXParseException if there is any error consuming any of
   *     originalSource's streams
   */
  CachedSaxInputSource(InputSource originalSource) throws SAXParseException {
    if (originalSource.getByteStream() != null) {
      setByteStream(CachedByteStream.from(originalSource.getByteStream()));
    }

    if (originalSource.getCharacterStream() != null) {
      setCharacterStream(
          CachedCharStream.from(originalSource.getCharacterStream()));
    }

    setPublicId(originalSource.getPublicId());
    setSystemId(originalSource.getSystemId());
    setEncoding(originalSource.getEncoding());
  }

  /** Resets streams to their initial positions. */
  public void reset() throws IOException {
    if (getCharacterStream() != null) {
      getCharacterStream().reset();
    }
    if (getByteStream() != null) {
      getByteStream().reset();
    }
  }

  /**
   * Wraps a ByteArrayOutputStream to provide direct access to the underlying
   * buffer.  We do this to avoid the default BAOS behavior, which is to make
   * a defensive copy.
   */
  private static class ByteBuffer extends ByteArrayOutputStream {
    ByteBuffer(int initialCapacity) {
      super(initialCapacity);
    }

    /**
     * Returns the underlying byte buffer, which <b>may be larger than
     * necessary</b>.  Callers must limit access to {@link #size} bytes.
     */
    synchronized byte[] getBytes() {
      return buf;
    }
  }

  private static class CachedByteStream extends ByteArrayInputStream {
    CachedByteStream(byte[] buf, int offset, int length) {
      super(buf, offset, length);
    }

    static CachedByteStream from(InputStream input) throws SAXParseException {
      try {
        ByteBuffer buffer = new ByteBuffer(CHUNK_SIZE);
        byte[] chunk = new byte[CHUNK_SIZE];
        int bytesRead = -1;
        while ((bytesRead = input.read(chunk)) > 0) {
          buffer.write(chunk, 0, bytesRead);
        }
        closeQuietly(input);
        return new CachedByteStream(buffer.getBytes(), 0, buffer.size());
      } catch (IOException e) {
        throw new SAXParseException("Error reading byte stream", null, e);
      }
    }

    @Override
    public void close() {
      // No-op: this way our data may be read over and over again.  Standard
      // GC will take care of cleanup.
      // (note: overridden for clarity; this is actually default BAIS behavior)
    }
  }

  private static class CachedCharStream extends StringReader {
    CachedCharStream(String string) {
      super(string);
    }

    static CachedCharStream from(Reader reader) throws SAXParseException {
      try {
        char[] chunk = new char[CHUNK_SIZE];
        StringBuilder buffer = new StringBuilder();
        int bytesRead = -1;
        while ((bytesRead = reader.read(chunk)) > 0) {
          buffer.append(chunk, 0, bytesRead);
        }
        closeQuietly(reader);
        return new CachedCharStream(buffer.toString());
      } catch (IOException e) {
        throw new SAXParseException("Error reading character stream", null, e);
      }
    }

    @Override
    public void close() {
      // Override default StringReader behavior to no-op: this way our data may
      // be read over and over again.  Standard GC will take care of cleanup.
    }
  }

  /**
   * Closes the given {@link Closeable} and squelches any resulting exception.
   */
  private static void closeQuietly(Closeable c) {
    try {
      c.close();
    } catch (IOException ignored) { }
  }
}
