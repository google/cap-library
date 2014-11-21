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

package com.google.publicalerts.cap.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.publicalerts.cap.CapUtil;
import com.google.publicalerts.cap.profile.CapProfile;

/**
 * Main servlet for the CAP validator.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class CapValidatorServlet extends HttpServlet {
  private static final long serialVersionUID = 7456177189138605168L;

  private static final Logger log =
      Logger.getLogger(CapValidatorServlet.class.getName());

  private final CapValidator capValidator;
  private final ServletFileUpload upload;

  public CapValidatorServlet() {
    this(new CapValidator(), new ServletFileUpload());
  }

  public CapValidatorServlet(
      CapValidator capValidator, ServletFileUpload upload) {
    this.capValidator = capValidator;
    this.upload = upload;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("input", "");
    req.setAttribute("profiles", ValidatorUtil.getProfilesJsp(null));
    render(req, resp);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String input = null;
    String fileInput = null;
    String example = null;
    Set<CapProfile> profiles = Sets.newHashSet();

    try {
      FileItemIterator itemItr = upload.getItemIterator(req);
      while (itemItr.hasNext()) {
        FileItemStream item = itemItr.next();
        if ("input".equals(item.getFieldName())) {
          input = ValidatorUtil.readFully(item.openStream());
        } else if (item.getFieldName().startsWith("inputfile")) {
          fileInput = ValidatorUtil.readFully(item.openStream());
        } else if ("example".equals(item.getFieldName())) {
          example = ValidatorUtil.readFully(item.openStream());
        } else if ("profile".equals(item.getFieldName())) {
          String profileCode = ValidatorUtil.readFully(item.openStream());
          profiles.addAll(ValidatorUtil.parseProfiles(profileCode));
        }
      }
    } catch (FileUploadException e) {
      throw new ServletException(e);
    }

    if (!CapUtil.isEmptyOrWhitespace(example)) {
      log.info("ExampleRequest: " + example);
      input = loadExample(example);
      profiles = ImmutableSet.of();
    } else if (!CapUtil.isEmptyOrWhitespace(fileInput)) {
      log.info("FileInput");
      input = fileInput;
    }

    input = (input == null) ? "" : input.trim();
    if ("".equals(input)) {
      log.info("EmptyRequest");
      doGet(req, resp);
      return;
    }

    ValidationResult result = capValidator.validate(input, profiles);

    req.setAttribute("input", input);
    req.setAttribute("profiles", ValidatorUtil.getProfilesJsp(profiles));
    req.setAttribute("validationResult", result);
    req.setAttribute("lines", Arrays.asList(result.getInput().split("\n")));
    req.setAttribute("timing", result.getTiming());
    JSONArray alertsJs =
        new MapVisualizer(result.getValidAlerts()).getAlertsJs();
    if (alertsJs != null) {
      req.setAttribute("alertsJs", alertsJs.toString());
    }
    render(req, resp);
  }

  private void render(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("analyticsId",
        System.getProperty(ValidatorUtil.GOOGLE_ANALYTICS_ID));
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("text/html");
    ValidatorUtil.addNoCachableHeaders(resp);

    resp.setStatus(HttpServletResponse.SC_OK);
    req.getRequestDispatcher("/validator.jsp").include(req, resp);
  }
  
  @VisibleForTesting
  String loadExample(String label) throws IOException {
    CapExample capExample = null;
    
    try {
      capExample = CapExample.fromLabel(label);
    } catch (IllegalArgumentException e) {
      log.severe(
          "Label: " + label + " does not match an existing CAP example.");
      return "";
    }
    
    InputStream stream = CapValidatorServlet.class
        .getResourceAsStream(capExample.getRelativePath());
    return stream == null ? "" : ValidatorUtil.readFully(stream);
  }
  
  /**
   * An example of a CAP message.
   * 
   * <p>The list should be kept in sync with the content of the package
   * {@code com.google.com.publicalerts.cap.validator.examples}.
   */
  public enum CapExample {
    SEVERE_THUNDERSTORM_WARNING(
        "CAP 1.2 Severe Thunderstorm Warning", "thunderstorm.cap"), 
    HOMELAND_SECURITY_ADVISORY(
        "CAP 1.2 Homeland Security Advisory", "homeland_security.url"),
    EARTHQUAKE_ATOM_FEED("CAP 1.1 Earthquake Atom feed", "earthquake.atom"),
    AMBER_ALERT_RSS_FEED("CAP 1.1 Amber Alert RSS feed", "amber.rss"),
    BUSHFIRE_EDXLDE_FEED("CAP 1.2 Rural Fire EDXL-DE feed", "bushfire.edxlde");
    
    private final String description;
    private final String fileName;
    
    private CapExample(String description, String fileName) {
      this.description = description;
      this.fileName = fileName;
    }

    public String getDescription() {
      return description;
    }
    
    private String getRelativePath() {
      return "examples/" + fileName;
    }

    public String getLabel() {
      return this.name().toLowerCase().replace('_', '-');
    }
    
    @VisibleForTesting
    static CapExample fromLabel(String label)
        throws IllegalArgumentException {
      return CapExample.valueOf(label.toUpperCase().replace('-', '_'));
    }
  }
}
