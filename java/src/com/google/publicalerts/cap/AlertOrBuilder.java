// Generated by the protocol buffer compiler.  DO NOT EDIT!

package com.google.publicalerts.cap;

public interface AlertOrBuilder
    extends com.google.protobuf.MessageOrBuilder {
  
  // required string xmlns = 1;
  boolean hasXmlns();
  String getXmlns();
  
  // required string identifier = 2;
  boolean hasIdentifier();
  String getIdentifier();
  
  // required string sender = 3;
  boolean hasSender();
  String getSender();
  
  // optional string password = 4 [deprecated = true];
  @java.lang.Deprecated boolean hasPassword();
  @java.lang.Deprecated String getPassword();
  
  // required string sent = 5;
  boolean hasSent();
  String getSent();
  
  // required .publicalerts.cap.Alert.Status status = 6;
  boolean hasStatus();
  com.google.publicalerts.cap.Alert.Status getStatus();
  
  // required .publicalerts.cap.Alert.MsgType msg_type = 7;
  boolean hasMsgType();
  com.google.publicalerts.cap.Alert.MsgType getMsgType();
  
  // optional string source = 8;
  boolean hasSource();
  String getSource();
  
  // optional .publicalerts.cap.Alert.Scope scope = 9;
  boolean hasScope();
  com.google.publicalerts.cap.Alert.Scope getScope();
  
  // optional string restriction = 10;
  boolean hasRestriction();
  String getRestriction();
  
  // optional .publicalerts.cap.Group addresses = 11;
  boolean hasAddresses();
  com.google.publicalerts.cap.Group getAddresses();
  com.google.publicalerts.cap.GroupOrBuilder getAddressesOrBuilder();
  
  // repeated string code = 12;
  java.util.List<String> getCodeList();
  int getCodeCount();
  String getCode(int index);
  
  // optional string note = 13;
  boolean hasNote();
  String getNote();
  
  // optional .publicalerts.cap.Group references = 14;
  boolean hasReferences();
  com.google.publicalerts.cap.Group getReferences();
  com.google.publicalerts.cap.GroupOrBuilder getReferencesOrBuilder();
  
  // optional .publicalerts.cap.Group incidents = 15;
  boolean hasIncidents();
  com.google.publicalerts.cap.Group getIncidents();
  com.google.publicalerts.cap.GroupOrBuilder getIncidentsOrBuilder();
  
  // repeated .publicalerts.cap.Info info = 16;
  java.util.List<com.google.publicalerts.cap.Info> 
      getInfoList();
  com.google.publicalerts.cap.Info getInfo(int index);
  int getInfoCount();
  java.util.List<? extends com.google.publicalerts.cap.InfoOrBuilder> 
      getInfoOrBuilderList();
  com.google.publicalerts.cap.InfoOrBuilder getInfoOrBuilder(
      int index);
}
