<%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page import="com.google.publicalerts.cap.validator.ValidationError"
%><%@ page import="com.google.publicalerts.cap.validator.StringUtil"
%><%@ page import="com.google.common.collect.Multimap"
%><%@ page import="java.util.List"
%><!DOCTYPE html>
<html>
<head>
<meta name="keywords" content="CAP,common alerting protocol,CAP validator,common alerting protocol validator">
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta http-equiv="content-language" content="en">
<title>Common Alerting Protocol Validator (beta)</title>
<link href="/style.css" type="text/css" rel="stylesheet">
</head>
<body>
<div class=header>
  <img src="/googleorg_logo.gif" alt="Google Labs" class="logo">
  <span class=title>Common Alerting Protocol Validator</span>
</div>
<div class=desc>The 
<a target=_blank href="http://en.wikipedia.org/wiki/Common_Alerting_Protocol">Common Alerting Protocol</a>
validator is a free service that checks the syntax of CAP XML messages and
Atom and RSS feeds of CAP messages.  It supports CAP v1.0, v1.1 and v1.2.
</div>
<form id=inputform action="/validate#r" method=post enctype="multipart/form-data">
<table cellpadding=0 cellspacing=0><tr><td>
<h4>Input feed</h4>
<div class=input><%
    String input = StringUtil.htmlEscape((String) request.getAttribute("input"));
    %><div id=inputdiv><textarea rows=28 cols=80 name=input><%= input %></textarea>
      <div class=selectfile>Type an alert or <a href="#" onclick="swapTextareaFile(false)">upload a file</a>.</div>
    </div>
    <div id=filediv class=filediv style="display:none">
      <div class=cancelfile><a href="#" onclick="swapTextareaFile(true)">Cancel</a></div>
      <input type=file name=inputfile/>
    </div>
    <a name="r"></a><%
    List<String[]> profiles = (List<String[]>) request.getAttribute("profiles");
    if (profiles != null) {
      %><div>
        <div class=profiles>
          <span>(Optional) Validate against common CAP profiles:</span>
          <table><% 
          for (String[] profile : profiles) {
            %><tr>
              <td><input type=checkbox name=profile value=<%= profile[0] %> <%=profile[1]%> /></td>
              <td><label for=<%= profile[0] %>><a href="<%= profile[2] %>" target=_blank><%= profile[3] %></a></label></td>
            </tr><%
          }
          %></table>
        </div>
      </div><%
    }
    %><input type=submit class=submit value="Validate"/>
</div>
</td><td valign=top>
  <div class=examples>
  <h4>Try these examples:</h4>
  <input type=hidden name=example id=example />
  <div class=example><a href="#" onclick="submitExample('thunderstorm.cap')">CAP 1.2 Severe Thunderstorm Warning</a></div>
  <div class=example><a href="#" onclick="submitExample('homeland_security.url')">CAP 1.2 Homeland Security Advisory</a></div>
  <div class=example><a href="#" onclick="submitExample('earthquake.atom')">CAP 1.1 Earthquake Atom feed</a></div>
  <div class=example><a href="#" onclick="submitExample('amber.rss')">CAP 1.1 Amber Alert RSS feed</a></div>
</div></td></tr></table>
</form>

<%
Multimap<Integer, ValidationError> errors = (Multimap<Integer, ValidationError>) request.getAttribute("errors");
if (errors != null) {
  if (errors.isEmpty()) {
    %><h4>Result</h4>
    <div class=valid>Valid!</div><%
    String alertsJs = (String) request.getAttribute("alertsJs");
    if (alertsJs != null) {
      %><div id=map style="margin: 1em; padding: 1ex;"></div>
      <script src="http://maps.google.com/maps/api/js?sensor=false"></script>
      <script>var alerts = <%= alertsJs %>;</script>
	  <script src="/map.js"></script><%
    }
  } else {
    %><h4>Error</h4>
    <div class=error>
      <table cellpadding=0 cellspacing=0><%
    for (ValidationError error : errors.values()) {
      %><tr>
        <td class=linenum><a href="#l<%= error.getLineNumber() %>"/><%= error.getLineNumber() %></td>
        <td class=line><%= error.getEscapedMessage() %></td>
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
  %><h4>Recommendations</h4>
     <div class=recommendation>
       <table cellpadding=0 cellspacing=0><%
  for (ValidationError recommendation : recommendations.values()) {
       %><tr>
           <td class=linenum><a href="#l<%= recommendation.getLineNumber() %>"/><%= recommendation.getLineNumber() %></td>
           <td class=line><%= recommendation.getEscapedMessage() %></td>
         </tr><%
  }
     %></table>
  </div><%
}
%>

<% 
List<String> lines = (List<String>) request.getAttribute("lines");
if (lines != null) {
  %><h4>File</h4>
  <div><div class=lines>
  <table cellpadding=0 cellspacing=0><%
  for (int i = 0; i < lines.size(); i++) {
    int line = i + 1;
    String cssClass = errors.containsKey(line) ? "errorline"
               : recommendations.containsKey(line) ? "recommendline"
               : "";
    %><tr class="<%= cssClass %>">
      <td class=linenum><a name="l<%= i+1 %>"/><%= i+1 %></td>
      <td class=line><%= StringUtil.htmlEscape(lines.get(i)) %></td>
    </tr><%
    for (ValidationError error : errors.get(line)) {
      %><tr class="errorline">
        <td class=linenum></td><td class="line errormsg"><%= error.getEscapedMessage() %></td>
      </tr><%
    }
    for (ValidationError recommendation : recommendations.get(line)) {
      %><tr class="recommendline">
        <td class=linenum></td><td class="line recommendmsg"><%= recommendation.getEscapedMessage() %></td>
      </tr><%
    }
  }
  %></table>
  </div></div><%
}
%>

<div class=footer>
  <p class="homeabout">&#xa9;2011 Google - <a href="http://www.google.com/accounts/TOS">Terms of Service</a> - <a href="http://code.google.com/p/cap-library">About the Common Alerting Protocol Validator</a> - <a href="http://www.google.com/intl/en/privacy.html">Privacy Policy</a></p>
  <img src="http://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine" height="30" width="120"/>
</div>

<%
String timing = (String) request.getAttribute("timing");
if (timing != null) {
%><div class=debugfooter>
<%= StringUtil.lineBreaksBr(StringUtil.htmlEscape(timing)) %>
</div><%
}
%>

<script type="text/javascript">
  function submitExample(example) {
    document.getElementById('example').value = example;
    document.getElementById('inputform').submit();
  }
  function swapTextareaFile(showTextarea) {
    var inputDiv = document.getElementById('inputdiv');
    var fileDiv = document.getElementById('filediv');
    inputDiv.style.display = showTextarea ? '' : 'none';
    fileDiv.style.display = showTextarea ? 'none' : '';
  }
</script>

<%
String analyticsId = (String) request.getAttribute("analyticsId");
if (analyticsId != null) {
%><script type="text/javascript">
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', '<%= analyticsId %>']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script');
    ga.type = 'text/javascript';
    ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www')
    		+ '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(ga, s);
  })();
</script><%
}
%>
</body>
</html>
