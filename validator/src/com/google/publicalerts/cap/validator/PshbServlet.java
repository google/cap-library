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

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.google.publicalerts.cap.profile.CapProfile;

/**
 * Servlet to accept POSTs of new alerts for validation.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class PshbServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger log =
      Logger.getLogger(PshbServlet.class.getName());

  private final PshbAuthenticator authenticator;
  private final CapValidator capValidator;
  private final MailSender mailSender;

  public PshbServlet() {
    this(new PshbAuthenticator(), new CapValidator(), new MailSender());
  }

  public PshbServlet(PshbAuthenticator authenticator,
      CapValidator capValidator, MailSender mailSender) {
    this.authenticator = authenticator;
    this.capValidator = capValidator;
    this.mailSender = mailSender;
  }

  /**
   * Handles PSHB subscription requests.
   *
   * See http://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.3.html#verifysub
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String mode = req.getParameter("hub.mode");
    String topic = req.getParameter("hub.topic");
    String verifyToken = req.getParameter("hub.verify_token");

    log.info("Received " + mode + " request for " + topic);

    if (!verifyPshbSubscription(mode, topic, verifyToken)) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    log.info("Confirming " + mode + " request for topic " + topic);
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getWriter().write(req.getParameter("hub.challenge"));
  }

  private boolean verifyPshbSubscription(
      String mode, String topic, String verifyToken) {
    if (!("subscribe".equals(mode) || "unsubscribe".equals(mode))) {
      log.warning("Rejecting subscription, invalid mode: " + mode);
      return false;
    }

    try {
      new URL(topic);
    } catch (MalformedURLException e) {
      log.warning("Rejecting subscription, invalid topic: " + topic);
      return false;
    }

    String validToken = System.getProperty(ValidatorUtil.ALERT_HUB_SECRET);
    if (!validToken.equals(verifyToken)) {
      log.warning(
          "Rejecting subscription, invalid verifyToken: " + verifyToken);
      return false;
    }

    return true;
  }

  /**
   * Handle POSTs from PSHB for new alerts to validate.
   *
   * See http://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.3.html#contentdistribution
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String input = CharStreams.toString(
        new InputStreamReader(req.getInputStream(), Charsets.UTF_8));
    String pshbSig = req.getHeader(PshbAuthenticator.SIGNATURE_HEADER);
    String topic = req.getParameter("topic");
    String email = req.getParameter("email");
    String profileStr = req.getParameter("profiles");

    PshbAuthenticator.AuthResult authResult = authenticator.authenticate(
        input, pshbSig,	System.getProperty(ValidatorUtil.ALERT_HUB_SECRET));
    if (authResult != PshbAuthenticator.AuthResult.OK) {
	resp.setStatus(HttpServletResponse.SC_OK);
	return;
    }

    // Reply to PSHB so the connection doesn't time out
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.flushBuffer();
    
    // The PSHB POST is valid, validate the input
    Set<CapProfile> profiles = ValidatorUtil.parseProfiles(profileStr);
    ValidationResult result = capValidator.validate(input, profiles);

    // If there are errors, send email
    if (result.containsErrors()) {
      String msg = renderMessage(req, resp, email, topic, profiles, result);
      sendMail(email, topic, msg, profiles, result);
    }

    // TODO(shakusa) Optionally send email if there are recommendations
    // TODO(shakusa) Maintain a dashboard by topic and update that here
  }

  private void sendMail(String toAddress, String topic, String msg,
        Set<CapProfile> profiles, ValidationResult result) {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage message = new MimeMessage(session);
    try {
      message.setFrom(new InternetAddress(
          "admin@cap-validator.appspotmail.com", "CAP Validator"));
      Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();
      for (String address : splitter.split(toAddress)) {
        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress(address));
      }
      message.setSubject("CAP Validator: Invalid CAP alerts from " + topic);
      message.setText(msg, "UTF-8", "html");
      log.info("about to send email to: " + toAddress);
      mailSender.sendMail(message);
    } catch (MessagingException e) {
      String err = "Could not send mail to: " + toAddress +
	  " Message:" + e.getMessage();
      log.log(Level.SEVERE, err, e);
    } catch (UnsupportedEncodingException e) {
      String err = "Could not send mail to: " + toAddress +
	  " Message:" + e.getMessage();
      log.log(Level.SEVERE, err, e);
    }
  }

  /**
   * Renders the validation result to a string to be included in the email.
   * To render, we re-use the same JSP used in the mail validator, but wrap
   * the HttpServletResponse to redirect and grab the string instead of
   * writing the output to the stream (since the request comes from
   * PSHB, we don't bother rendering it to the stream as well).
   * 
   * @param req the current request
   * @param resp the current response
   * @param email where to send the mail
   * @param topic the current topic
   * @param profiles the set of optional profiles to validate against
   * @param result the valiation result
   * @return the rendered validation result
   */
  private String renderMessage(
      HttpServletRequest req, HttpServletResponse resp, String email,
      String topic, Set<CapProfile> profiles, ValidationResult result) {

    // Write the jsp to a string so we can include it in the email
    ToStringWrapper wrapper = new ToStringWrapper(resp);

    String url = req.getRequestURL().toString();
    String unsubscribeUrl = ValidatorUtil.toCallbackUrl(
         url.replace("/pshb", "/subscribe"), topic, email);

    req.setAttribute("url", url.replace("/pshb", ""));
    req.setAttribute("unsubscribe", unsubscribeUrl);
    req.setAttribute("profiles", ValidatorUtil.getProfilesJsp(profiles));
    req.setAttribute("validationResult", result);
    req.setAttribute("lines", Arrays.asList(result.getInput().split("\n")));

    try {
      req.getRequestDispatcher("/subscription_email.jsp")
	  .include(req, wrapper);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ServletException e) {
      throw new RuntimeException(e);
    }

    return wrapper.toString();
  }

  /** HttpServletResponseWrapper that writes to a ByteArrayOutputStream */
  private static class ToStringWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream baos;
    private final ToStringOutputStream tsos;
    private final PrintWriter writer;

    public ToStringWrapper(HttpServletResponse resp) {
      super(resp);
      this.baos = new ByteArrayOutputStream();
      this.tsos = new ToStringOutputStream(baos);
      this.writer = new PrintWriter(baos, true /* auto-flush */);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
      writer.flush();
      return tsos;
    }

    @Override
    public String toString() {
      writer.flush();
      return baos.toString();
    }
  }

  /** ServletOutputStream that writes to a ByteArrayOutputStream */
  private static class ToStringOutputStream extends ServletOutputStream {
    private final ByteArrayOutputStream baos;

    public ToStringOutputStream(ByteArrayOutputStream baos) {
      this.baos = baos;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      baos.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
      baos.write(b);
    }

    @Override
    public void write(int b) throws IOException {
      baos.write(b);
    }
  }
}
