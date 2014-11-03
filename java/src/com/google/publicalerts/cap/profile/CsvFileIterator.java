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

package com.google.publicalerts.cap.profile;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Provides an Iterator interface to a csv file. Each call to getNext()
 * returns a {@link CsvRow} from which to extract the data.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CsvFileIterator implements Iterator<CsvFileIterator.CsvRow> {
  private final BufferedReader input;

  private String[] nextRow;

  public CsvFileIterator(InputStream stream) {
    this.input = new BufferedReader(new InputStreamReader(stream));
  }

  @Override
  public boolean hasNext() {
    maybeCacheNextRow();
    return nextRow != null;
  }

  @Override
  public CsvRow next() {
    maybeCacheNextRow();
    if (nextRow == null) {
      throw new NoSuchElementException();
    }
    String[] tmp = nextRow;
    nextRow = null;
    return new CsvRow(tmp);
  }

  private void maybeCacheNextRow() {
    if (nextRow == null) {
      try {
        nextRow = readNextRow();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String[] readNextRow() throws IOException {
    String line = input.readLine();
    if (line == null) {
      return null;
    }
    return splitRow(line);
  }

  String[] splitRow(CharSequence line) {
    List<String> cellBuffer = Lists.newArrayList();
    int cellStart = 0;
    int cellEnd;
    int len = line.length();
    CharSequence cellSequence;

    while (cellStart <= len) {
      cellEnd = findCellEnd(line, cellStart);
      if (cellEnd == -1) {
        cellEnd = len;
      }
      cellSequence = line.subSequence(cellStart, cellEnd);
      cellBuffer.add(unescape(cellSequence));
      cellStart = cellEnd + 1;
    }

    return cellBuffer.toArray(new String[cellBuffer.size()]);
  }

  /**
   * Given the offset of the beginning of a field, this returns the offset
   * of the character after end of the field.<p>
   * For example, with the
   * string '<code>abc,"hey, you",12345</code>':<pre>
   * Offset  Result  Field described
   * 0       3       abc
   * 1       3       bc
   * 3       3       (zero-length string)
   * 4       14      "hey, you"
   * 5       8       hey   --because the opening quote was skipped
   * 13      -1      (starting at the quote, no matching endquote)
   * 15      20      12345
   * 20      20      (zero-length string)
   * 21      -1      (null)
   * </pre>
   * @return -1 if the cell does not terminate before the end of the string
   */
  private int findCellEnd(CharSequence str, int offset) {
    int len = str.length();
    if (offset == len) {
      return offset;
    }
    if (offset > len) {
      return -1;
    }

    char c = str.charAt(offset++);

    // If it starts with a quote, read until a matching quote.
    if (c == '"') {
      while (true) {
        // the line has ended before a closing quote was found.
        if (offset >= len) {
          return -1;
        }

        // get the next character
        c = str.charAt(offset++);

        // if the next character is anything but a quote, keep reading
        if (c != '"') {
          continue;
        }

        // We just read a quote.  If that was the last character on the
        // line, then the cell ended right at the end of the line and
        // all is good.
        if (offset >= len) {
          break;
        }

        c = str.charAt(offset++);

        // If the next character is also a quote, skip it and continue
        // the quoted section.  Otherwise, this is the end of the
        // quoted section.
        if (c != '"') {
          break;
        }
      }
    }

    // read the unquoted part of the field
    while (true) {
      if (c == ',') {
        return offset - 1;
      }

      if (offset >= len) {
        return offset;
      }

      c = str.charAt(offset++);
    }
  }

  String unescape(CharSequence str) {
    int len = str.length();

    // no string can be interesting with only 1 character
    if (len < 2) {
      return str.toString();
    }

    // if there is no quoted section, there isn't anything
    // interesting either
    if (str.charAt(0) != '"') {
      return str.toString();
    }


    StringBuffer buf = new StringBuffer();
    int offset = 1;  // set to 1 to skip the opening quote
    char c;

    // extract the optional quoted section at the beginning
    while (true) {
      // we reached the end of the string without finding a closing
      // quote.  Oh well, end the string anyway.
      if (offset >= len) {
        break;
      }

      c = str.charAt(offset++);

      if (c != '"') {
        buf.append(c);
        continue;
      }

      // we just found a quote.

      // if it's the end of the string, we're done
      if (offset >= len) {
        break;
      }

      // If the quote is followed by another quote it's an escaped
      // quote character.
      // If the quote is alone, it's the end of the quoted section.
      c = str.charAt(offset++);
      buf.append(c);
      if (c != '"') {
        break;
      }
    }

    while (offset < len) {
      buf.append(str.charAt(offset++));
    }

    return buf.toString();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static class CsvRow {
    private final String[] cells;

    public CsvRow(String[] cells) {
      this.cells = cells;
    }

    public String[] getCells() {
      return cells;
    }

    /**
     * Returns the value of the given cells at the given index, or
     * {@code defaultValue} if the index does not exist.
     *
     * @param index the index to pull from the cells
     * @param defaultValue the default value if index does not exist in the cells
     * @return row[index] or default
     */
    public String getAt(int index, String defaultValue) {
      if (index < 0 || index >= cells.length) {
        return defaultValue;
      }
      return cells[index];
    }
  }
}
