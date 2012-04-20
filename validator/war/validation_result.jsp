<%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page import="com.google.publicalerts.cap.validator.ValidationError"
%><%@ page import="com.google.publicalerts.cap.validator.StringUtil"
%><%@ page import="com.google.common.collect.Multimap"
%><%@ page import="java.util.List"
%>
<div style="color: #333; font-family: arial, sans-serif; font-size: 13px">
<%
Multimap<Integer, ValidationError> errors = (Multimap<Integer, ValidationError>) request.getAttribute("errors");
if (errors != null) {
  if (errors.isEmpty()) {
    %><h4 style="font-size:120%">Result</h4>
    <div style="font-size:120%;background-color: #cfc;display: inline-block;margin-left: 1em;padding: 1ex;">Valid!</div><%
    String alertsJs = (String) request.getAttribute("alertsJs");
    if (alertsJs != null) {
      %><div id=map style="margin: 1em; padding: 1ex;"></div>
      <script src="http://maps.google.com/maps/api/js?sensor=false"></script>
      <script>var alerts = <%= alertsJs %>;</script>
	  <script src="/map.js"></script><%
    }
  } else {
    %><h4 style="font-size:120%">Error</h4>
    <div style="background-color: #fcc;display: inline-block;margin-left: 1em;padding: 1ex;">
      <table cellpadding=0 cellspacing=0><%
    for (ValidationError error : errors.values()) {
      %><tr>
        <td style="padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"><a href="#l<%= error.getLineNumber() %>"/><%= error.getLineNumber() %></td>
        <td style="padding-right: 4px;"><%= error.getEscapedMessage() %></td>
      </tr><%
    }
    %></table>
  </div><%
  }
}
%>

<%
Multimap<Integer, ValidationError> recommendations = (Multimap<Integer, ValidationError>) request.getAttribute("recommendations");
if (recommendations != null && !recommendations.isEmpty()) {
  %><h4 style="font-size:120%">Recommendations</h4>
     <div style="background-color: #ccf;display: inline-block;margin-left: 1em;padding: 1ex;">
       <table cellpadding=0 cellspacing=0><%
  for (ValidationError recommendation : recommendations.values()) {
       %><tr>
           <td style="padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"><a href="#l<%= recommendation.getLineNumber() %>"/><%= recommendation.getLineNumber() %></td>
           <td style="padding-right: 4px;"><%= recommendation.getEscapedMessage() %></td>
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
    String style = errors.containsKey(line) ? "background-color: #fcc;"
               : recommendations.containsKey(line) ? "background-color: #ccf;"
               : "";
    %><tr style="<%= style %>">
      <td style="<%= style %>padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"><a name="l<%= i+1 %>"/><%= i+1 %></td>
      <td style="<%= style %>display: inline-block;margin-left: 1em;padding: 1px;"><%= StringUtil.htmlEscape(lines.get(i)) %></td>
    </tr><%
    for (ValidationError error : errors.get(line)) {
      %><tr style="background-color: #fcc;">
        <td style="background-color: #fcc;padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"></td><td style="background-color: #fcc;display: inline-block;margin-left: 1em;padding: 1ex;font-weight: bold;"><%= error.getEscapedMessage() %></td>
      </tr><%
    }
    for (ValidationError recommendation : recommendations.get(line)) {
      %><tr style="background-color: #ccf;">
        <td style="background-color: #ccf;padding-left: 4px;padding-right: 6px;text-align: right;vertical-align: top;"></td><td style="background-color: #ccf;display: inline-block;margin-left: 1em;padding: 1ex; padding: 4px;"><%= recommendation.getEscapedMessage() %></td>
      </tr><%
    }
  }
  %></table>
  </div></div><%
}
%>
</div>