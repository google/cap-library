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

import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

/**
 * A class that sends mail.
 *
 * @author shakusa@google.com (Steve Hakusa)
 */
public class MailSender {
  private static final Logger log =
      Logger.getLogger(MailSender.class.getName());

  /**
   * Sends mail.
   *
   * @param message the message to send
   */
  public void sendMail(Message message) {
    try {
      Transport.send(message);
    } catch (MessagingException e)  {
      throw new RuntimeException(e);
    }
  }
}
