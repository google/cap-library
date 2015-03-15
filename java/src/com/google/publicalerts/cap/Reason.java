/*
 * Copyright (C) 2014 Google Inc.
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.publicalerts.cap.feed.CapFeedException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Diagnostic information about a CAP message. Possibly, a reason behind a {@link CapException} or
 * {@link CapFeedException}.
 * 
 * <p>This class is immutable and thread-safe.
 * 
 * @author sschiavoni@google.com (Stefano Schiavoni)
 */
public class Reason {
  
  /**
   * Level of a {@link Reason}, to describe its severity.
   * 
   * <p>Values MUST be defined in ascending order of severity.
   */
  public enum Level {
    INFO, RECOMMENDATION, WARNING, ERROR;
    
    /**
     * @return the levels higher than {@code this}, in order of ascending severity
     */
    public List<Level> getHigherLevels() {
      return ImmutableList.copyOf(Arrays.copyOfRange(values(), ordinal() + 1, values().length));
    }
  }
  
  /**
   * Type of a {@link Reason}. Should allow equality comparison.
   * 
   * <p>This interface allows for types to be defined externally.
   */
  public static interface Type {
    
    /**
     * @return the localized message for this type (currently, English-only)
     */
    String getMessage(Locale locale);
    
    /**
     * @return the default level for this type
     */
    Level getDefaultLevel();
    
    /**
     * @return a string describing the source of this type of diagnostic information, e.g., a
     * profile code
     */
    String getSource();
  }

  /**
   * XPath expression to what the reason refers to.
   */
  private final String xPath;
  
  /**
   * Type of the reason.
   */
  private final Type type;

  /**
   * Message parameters for the reason.
   */
  private final Object[] messageParams;

  /**
   * Builds a {@link Reason} object.
   * 
   * @param xPath the fully-predicated XPath expression to what the reason refers to
   * (e.g., <pre>/alert[1]/info[1]</pre>)
   * @param type the type of the reason
   * @param messageParams message parameters for the reason
   */
  public Reason(String xPath, Type type, Object... messageParams) {
    this.xPath = checkNotNull(xPath);
    this.type = checkNotNull(type);
    this.messageParams = messageParams;
  }

  /**
   * Builds a copy of this {@link Reason}, with the XPath prefixed with {@code xPathPrefix}.
   * 
   * <p>For instance, if the this reason has XPath <pre>/alert[1]</pre>, and {@code xPathPrefix}
   * is <pre>/feed[1]/entry[1]</code>, the output reason will have XPath
   * <pre>/feed[1]/entry[1]/alert[1]</pre>. The {@code xPathPrefix} is expected to be
   * fully-predicated.
   * 
   * <p>This method should be used when an XML file is validated recursively, and the diagnostic
   * information collected about an XML fragment needs to be changed so that it references the
   * fragment within the XML file.
   */
  public Reason prefixWithXpath(String xPathPrefix) {
    checkNotNull(xPathPrefix);
    return new Reason(xPathPrefix + xPath, type, messageParams);
  }
  
  /**
   * @return the XPath expression to what the reason refers to
   */
  public String getXPath() {
    return xPath;
  }
  
  /**
   * @return the type of this reason
   */
  public Type getType() {
    return type;
  }

  /**
   * @return the level of this reason
   */
  public Level getLevel() {
    return type.getDefaultLevel();
  }

  /**
   * @return a string describing the source of this diagnostic information, e.g., a profile code
   */
  public String getSource() {
    return type.getSource();
  }

  /**
   * @return the {@code i}th message parameter, or {@code null} if {@code i} is out of bounds
   */
  public Object getMessageParam(int i) {
    return (messageParams.length > i) ? messageParams[i] : null;
  }
  
  /**
   * Returns the {@link Type}'s message formatted with the
   * {@link #messageParams}.
   * 
   * @return an English human-readable message for this reason
   */
  public String getMessage() {
    return getLocalizedMessage(Locale.ENGLISH);
  }
  
  /**
   * Returns the {@link Type}'s message formatted with the {@link #messageParams}.
   * 
   * @param locale for the requested message
   * @return a human-readable message for this reason
   */
  public String getLocalizedMessage(Locale locale) {
    if (messageParams.length == 0) {
      return type.getMessage(locale);
    }

    return MessageFormat.format(type.getMessage(locale), messageParams);
  }

  @Override
  public String toString() {
    return "[" + xPath + "] " + getMessage();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Reason)) {
      return false;
    }

    Reason that = (Reason) other;
    return Objects.equal(type, that.type)
        && Objects.equal(xPath, that.xPath)
        && Objects.equal(getLevel(),  that.getLevel())
        && Arrays.deepEquals(messageParams, that.messageParams);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, xPath, getLevel(), Arrays.hashCode(messageParams));
  }
}
