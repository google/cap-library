// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.profile;

import java.util.Date;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.google.publicalerts.cap.Alert;
import com.google.publicalerts.cap.CapException;
import com.google.publicalerts.cap.CapException.Reason;
import com.google.publicalerts.cap.CapException.ReasonType;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.CapXmlParser;
import com.google.publicalerts.cap.NotCapException;

/**
 * Abstract base class for CAP profiles.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public abstract class AbstractCapProfile extends CapXmlParser implements CapProfile {

  public AbstractCapProfile() {
    this(false);
  }

  public AbstractCapProfile(boolean strictXsdValidation) {
    super(true /* validate */, strictXsdValidation);
  }

  @Override
  public Alert parseFrom(InputSource is)
      throws CapException, NotCapException, SAXParseException {
    Alert alert = super.parseFrom(is);
    List<Reason> reasons = checkForErrors(alert);
    if (!reasons.isEmpty()) {
      throw new CapException(reasons);
    }
    return alert;
  }

  /**
   * Checks if the timezone on the given {@code dateStr} is zero.  If so, adds
   * a new reason with the given xpath and type to the list of reasons.
   *
   * @param reasons list to add to if timezone is zero
   * @param dateStr date string to check
   * @param xpath xpath of the element
   * @param type type of the error to add if timezone is zero
   */
  protected void checkZeroTimezone(List<Reason> reasons,
      String dateStr, String xpath, ReasonType type) {
    if (!CapUtil.isValidDate(dateStr)) {
      return;
    }
    if (CapUtil.getTimezoneOffset(dateStr) == 0) {
      reasons.add(new Reason(xpath, type));
    }
  }
}
