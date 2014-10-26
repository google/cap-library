<%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page import="com.google.publicalerts.cap.validator.ValidationMessage"
%><%@ page import="com.google.publicalerts.cap.validator.ValidationResult"
%><%@ page import="com.google.publicalerts.cap.validator.StringUtil"
%><%@ page import="com.google.common.collect.Multimap"
%><%@ page import="java.util.List"
%>
<div style="color: #333; font-family: arial, sans-serif; font-size: 13px">
<%
ValidationResult validationResult = (ValidationResult) request.getAttribute("validationResult");

if (validationResult != null) {
if (!validationResult.containsErrors()) {%>
  <h4>Result</h4>
  <div style="font-size:120%; background-color: #cfc; display: inline-block; margin-left: 1em; padding: 1ex;">Valid!</div><%
    String alertsJs = (String) request.getAttribute("alertsJs");
    if (alertsJs != null) {
      %><div id=map style="margin: 1em; padding: 1ex;"></div>
      <script src="http://maps.google.com/maps/api/js?sensor=false"></script>
      <script>var alerts = <%= alertsJs %>;</script>
    <script src="/map.js"></script><%
    }
}

Multimap<Integer, ValidationMessage> validationMessages = validationResult.getByLineValidationMessages();

if (!validationMessages.isEmpty()) {%>
  <h4 style="font-size:120%">Validation messages</h4>
    <div style="background-color: #fcc;display: inline-block;margin-left: 1em;padding: 1ex;">
      <table cellpadding=0 cellspacing=0><%
    for (ValidationMessage message : validationMessages.values()) {
      %><tr>
        <td style="padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"><a href="#l<%= message.getLineNumber() %>"/><%= message.getLineNumber() %></td>
        <td style="padding-right: 4px;">[<%= message.getLevel() %>] <%= message.getEscapedMessage() %></td>
      </tr><%
    }
    %></table>
  </div><%
}
%>

<% 
List<String> lines = (List<String>) request.getAttribute("lines");
if (lines != null) {
  %><h4 style="font-size:120%">File</h4>
  <div><div style="background-color: #ffc;display: inline-block;margin-left: 1em;padding: 1ex;">
  <table cellpadding=0 cellspacing=0><%
  for (int i = 0; i < lines.size(); i++) {
    int line = i + 1;
    String style = validationMessages.containsKey(line) ? "background-color: #fcc;" : "";
    %><tr style="<%= style %>">
      <td style="<%= style %>padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"><a name="l<%= i+1 %>"/><%= i+1 %></td>
      <td style="<%= style %>display: inline-block;margin-left: 1em;padding: 1px;"><%= StringUtil.htmlEscape(lines.get(i)) %></td>
    </tr><%
    for (ValidationMessage message : validationMessages.get(line)) {
      %><tr style="background-color: #fcc;">
        <td style="background-color: #fcc;padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"></td><td style="background-color: #fcc;display: inline-block;margin-left: 1em;padding: 1ex;font-weight: bold;">[<%= message.getLevel() %>] <%= message.getEscapedMessage() %></td>
      </tr><%
    }
  }
  %></table>
  </div></div><%
}
}
%>


</div>