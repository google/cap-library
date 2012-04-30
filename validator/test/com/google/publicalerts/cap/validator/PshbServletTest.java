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

package com.google.publicalerts.cap.validator;

import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.publicalerts.cap.testing.TestResources;
import com.google.publicalerts.cap.profile.CapProfile;

import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import junit.framework.TestCase;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link PshbServlet}.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class PshbServletTest extends TestCase {

  private PshbAuthenticator auth;
  private CapValidator capValidator;
  private MailSender mailSender;
  private PshbServlet servlet;

  private HttpServletRequest req;
  private HttpServletResponse resp;

  @Override
  public void setUp() throws Exception {
    auth = mock(PshbAuthenticator.class);
    capValidator = mock(CapValidator.class);
    mailSender = mock(MailSender.class);
    servlet = new PshbServlet(auth, capValidator, mailSender);

    req = mock(HttpServletRequest.class);
    resp = mock(HttpServletResponse.class);
    System.setProperty(ValidatorUtil.ALERT_HUB_SECRET, "secret");
  }

  public void testConfirmSubscription() throws Exception {
    runTestConfirmSubscription(HttpServletResponse.SC_OK,
        "subscribe", "http://feed.com/feed", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_OK,
        "subscribe", "http://feed.com/feed", "secret");

    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "foo", "http://feed.com/feed", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", "foo", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", "http://feed.com/feed", "foo");

    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        null, "http://feed.com/feed", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", null, "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", "http://feed.com/feed", null);

    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "", "http://feed.com/feed", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", "", "secret");
    runTestConfirmSubscription(HttpServletResponse.SC_NOT_FOUND,
        "subscribe", "http://feed.com/feed", "");
  }

  public void runTestConfirmSubscription(
      int expectedStatus, String mode, String topic, String verifyToken)
      throws Exception {
    req = mock(HttpServletRequest.class);
    resp = mock(HttpServletResponse.class);
    when(req.getParameter("hub.mode")).thenReturn(mode);
    when(req.getParameter("hub.topic")).thenReturn(topic);
    when(req.getParameter("hub.verify_token")).thenReturn(verifyToken);
    when(req.getParameter("hub.challenge")).thenReturn("challenge");
    PrintWriter writer = mock(PrintWriter.class);
    when(resp.getWriter()).thenReturn(writer);
    servlet.doGet(req, resp);
    verify(resp).setStatus(expectedStatus);
  }

  public void testPshbPost_success() throws Exception {
    String cap = TestResources.load("earthquake.cap");

    when(req.getInputStream()).thenReturn(new FakeServletInputStream(
        new ByteArrayInputStream(cap.getBytes("UTF-8"))));
    when(req.getParameter("profiles")).thenReturn(null);
    when(req.getParameter("topic")).thenReturn("http://topic.com/feed");
    when(req.getParameter("email")).thenReturn("test@example.com");

    when(auth.authenticate(eq(cap), any(String.class), any(String.class)))
	.thenReturn(PshbAuthenticator.AuthResult.OK);

    ValidationResult result = new ValidationResult(cap);
    result.addError(3, 0, "Error");
    when(capValidator.validate(eq(cap), anySetOf(CapProfile.class)))
        .thenReturn(result);

    when(req.getRequestURL())
        .thenReturn(new StringBuffer("http://localhost/pshb"));
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(req.getRequestDispatcher(any(String.class))).thenReturn(dispatcher);
 
    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);

    ArgumentCaptor<MimeMessage> messageCaptor =
        ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).sendMail(messageCaptor.capture());

    MimeMessage message = messageCaptor.getValue();
    assertEquals(new InternetAddress("test@example.com"),
        message.getRecipients(Message.RecipientType.TO)[0]);
    assertTrue(message.getSubject().contains("http://topic.com/feed"));
  }

  public void testPshbPost_noErrorsSendsNoMail() throws Exception {
    String cap = TestResources.load("earthquake.cap");

    when(req.getInputStream()).thenReturn(new FakeServletInputStream(
        new ByteArrayInputStream(cap.getBytes("UTF-8"))));
    when(req.getParameter("profiles")).thenReturn(null);
    when(req.getParameter("topic")).thenReturn("http://topic.com/feed");
    when(req.getParameter("email")).thenReturn("test@example.com");

    when(auth.authenticate(eq(cap), any(String.class), any(String.class)))
	.thenReturn(PshbAuthenticator.AuthResult.OK);

    ValidationResult result = new ValidationResult(cap);
    when(capValidator.validate(eq(cap), anySetOf(CapProfile.class)))
        .thenReturn(result);
 
    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    verify(mailSender, never()).sendMail(any(Message.class));
  }

  public void testPshbPost_authFail() throws Exception {
    String cap = TestResources.load("earthquake.cap");

    when(req.getInputStream()).thenReturn(new FakeServletInputStream(
        new ByteArrayInputStream(cap.getBytes("UTF-8"))));
    when(req.getParameter("profiles")).thenReturn(null);
    when(req.getParameter("topic")).thenReturn("http://topic.com/feed");
    when(req.getParameter("email")).thenReturn("test@example.com");

    when(auth.authenticate(eq(cap), any(String.class), any(String.class)))
	.thenReturn(PshbAuthenticator.AuthResult.NO_MATCH);

    servlet.doPost(req, resp);

    verify(resp).setStatus(HttpServletResponse.SC_OK);
    verify(capValidator, never())
        .validate(eq(cap), anySetOf(CapProfile.class));
    verify(mailSender, never()).sendMail(any(Message.class));
  }

  static class FakeServletInputStream extends ServletInputStream {
    private final InputStream source;

    public FakeServletInputStream(InputStream source) {
      this.source = source;
    }

    @Override
    public int read() throws IOException {
      return source.read();
    }

    @Override
    public int read(byte b[]) throws IOException {
      return source.read(b);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
      return source.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return source.skip(n);
    }

    @Override
    public int available() throws IOException {
      return source.available();
    }

    @Override
    public void close() throws IOException {
      source.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      source.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      source.reset();
    }

    @Override
    public boolean markSupported() {
      return source.markSupported();
    }
  }
}
