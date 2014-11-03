<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="com.google.publicalerts.cap.validator.ValidationMessage"%>
<%@ page import="com.google.publicalerts.cap.validator.ValidationResult"%>
<%@ page import="com.google.publicalerts.cap.validator.StringUtil"%>
<%@ page import="com.google.common.collect.Multimap"%>
<%@ page import="java.util.List"%>


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
<table class="linesTable">
<%
    for (ValidationMessage message : validationMessages.values()) {
      String cssClass = message.getLevel().name().toLowerCase();
%>
  <tr>
    <td class="<%=cssClass%>-dark lineNumberCell"><pre><a href="#l<%=message.getLineNumber()%>"><%=message.getLineNumber()%></a></pre></td>
    <td class="<%=cssClass%>"><p><strong><%=message.getLevel()%></strong> | <em><%=message.getSource()%></em><br><%=message.getEscapedMessage()%></p></td>
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
<table class="linesTable">

<%
    for (int i = 0; i < lines.size(); i++) {
      int lineNumber = i + 1;
      String cssClass = validationMessages.containsKey(lineNumber)
          ? validationMessages.get(lineNumber).iterator().next().getLevel().name().toLowerCase()
          : "normal";
%>
  <tr>
    <td class="<%=cssClass%>-dark lineNumberCell"><a name="l<%=lineNumber%>"/><pre><%=lineNumber%></pre></td>
    <td class="<%=cssClass%>"><pre><%=StringUtil.htmlEscape(lines.get(i))%></pre></td>
  </tr>
<%
      for (ValidationMessage message : validationMessages.get(lineNumber)) {
%>
  <tr>
    <td class="<%=cssClass%>-dark"></td>
    <td class="<%=cssClass%>"><p><strong><%=message.getLevel()%></strong> | <em><%=message.getSource()%></em><br><%=message.getEscapedMessage()%></p></td>
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
