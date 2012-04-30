// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.publicalerts.cap.edxl.types;

/**
 * Bean to hold <nonXmlContent> data from EDXL-DE feeds.
 *
 * @author anshul@google.com (Anshul Kundani)
 */
public class NonXmlContent {
  private String mimeType;
  private int size;
  private String digest;
  private String uri;
  private byte[] contentData;

  public NonXmlContent(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getDigest() {
    return digest;
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public byte[] getContentData() {
    return contentData;
  }

  public void setContentData(byte[] contentData) {
    this.contentData = contentData;
  }
}
