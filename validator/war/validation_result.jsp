<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="com.google.publicalerts.cap.validator.ValidationMessage"%>
<%@ page import="com.google.publicalerts.cap.validator.ValidationResult"%>
<%@ page import="com.google.publicalerts.cap.validator.StringUtil"%>
<%@ page import="com.google.publicalerts.cap.Reason.Level"%>
<%@ page import="com.google.common.collect.Multimap"%>
<%@ page import="java.util.List"%>

<%-- NOTE! This template is used in an email as well as in a web page, so styles must remain inline. --%>

<%!
String getColorForLevel(Level level, boolean dark) {
  if (dark) {
    switch (level) {
      case INFO: 
        return "#4db6ac";
      case RECOMMENDATION:
        return "#91a7ff";
      case WARNING:
        return "#ffb74d";
      default:
        return "#f36c60";
      }
  } else {
    switch(level) {
      case INFO:
        return "#80cbc4";
      case RECOMMENDATION:
        return "#afbfff";
      case WARNING:
        return "#ffcc80";
      default:
        return "#f69988";
    }
  }
}

%>
<%
ValidationResult validationResult = (ValidationResult) request.getAttribute("validationResult");

if (validationResult != null) {
  if (!validationResult.containsErrors()) {
%>
<h4>Result</h4>
<div style="font-size:120%; background-color: #cfc; display: inline-block; margin-left: 1em; padding: 1ex;">Valid!</div>
<%
    String alertsJs = (String) request.getAttribute("alertsJs");
    if (alertsJs != null) {
%>
<div id=map style="margin: 1em; padding: 1ex;"></div>
<script src="http://maps.google.com/maps/api/js?sensor=false"></script>
<script>var alerts = <%= alertsJs %>;</script>
<script src="/map.js"></script>
<%
    }
  }

  Multimap<Integer, ValidationMessage> validationMessages = validationResult.getByLineValidationMessages();

  if (!validationMessages.isEmpty()) {
%>
<h4>Validation messages</h4>
<table style="border-spacing:0; width:800px; padding-left:1em;">
<%
    for (ValidationMessage message : validationMessages.values()) {
      String darkColor = getColorForLevel(message.getLevel(), true);
      String color = getColorForLevel(message.getLevel(), false);
%>
  <tr>
    <td style="padding:0.3em; width:3em; text-align:right; vertical-align:top; background-color:<%=darkColor%>;">
      <pre style="margin:0px; padding:0px; white-space: pre-wrap;"><a href="#l<%=message.getLineNumber()%>" style="color:black;"><%=message.getLineNumber()%></a></pre>
    </td>
    <td style="padding:0.3em; background-color:<%=color%>;">
      <p style="font-size:14px; margin:0;">
        <strong><%=message.getLevel()%></strong> | <em><%=message.getSource()%></em><br><%=message.getEscapedMessage()%>
      </p>
    </td>
  </tr>
<%
    }
%>
</table>
<%
  }
%>

<% 
  List<String> lines = (List<String>) request.getAttribute("lines");
  if (lines != null) {
%>

<h4>File</h4>
<table style="border-spacing:0; width:800px; padding-left:1em;">

<%
    for (int i = 0; i < lines.size(); i++) {
      int lineNumber = i + 1;
      String darkColor = validationMessages.containsKey(lineNumber)
          ? getColorForLevel(validationMessages.get(lineNumber).iterator().next().getLevel(), true)
          : "#e0e0e0";
      String color = validationMessages.containsKey(lineNumber)
          ? getColorForLevel(validationMessages.get(lineNumber).iterator().next().getLevel(), false)
          : "#eeeeee";
%>
  <tr>
    <td style="padding:0.3em; width:3em; text-align:right; vertical-align:top; background-color:<%=darkColor%>;">
      <a name="l<%=lineNumber%>"/>
      <pre style="margin:0px; padding:0px; white-space: pre-wrap;"><%=lineNumber%></pre>
    </td>
    <td style="padding:0.3em; background-color:<%=color%>;">
      <pre style="margin:0px; padding:0px; white-space: pre-wrap;"><%=StringUtil.htmlEscape(lines.get(i))%></pre>
    </td>
  </tr>
<%
      for (ValidationMessage message : validationMessages.get(lineNumber)) {
          darkColor = getColorForLevel(message.getLevel(), true);
          color = getColorForLevel(message.getLevel(), false);
%>
  <tr>
    <td style="padding:0.3em; width:3em; text-align:right; vertical-align:top; background-color:<%=darkColor%>;"></td>
    <td style="padding:0.3em; background-color:<%=color%>;">
      <p style="font-size:14px; margin:0;">
        <strong><%=message.getLevel()%></strong> | <em><%=message.getSource()%></em><br><%=message.getEscapedMessage()%>
      </p>
    </td>
  </tr>
<%
      }
    }
%>
</table>
<%
  }
}
%>
