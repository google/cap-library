<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="com.google.publicalerts.cap.validator.StringUtil"%>
<%@ page import="com.google.publicalerts.cap.validator.CapValidatorServlet"%>
<%@ page import="java.util.List"%>
<!DOCTYPE html>
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
  <% for (CapValidatorServlet.CapExample capExample : CapValidatorServlet.CapExample.values()) { %>  
  <div class=example><a href="#" onclick="submitExample('<%= capExample.getLabel() %>')"><%= StringUtil.htmlEscape(capExample.getDescription()) %></a></div>
  <% } %>
</div></td></tr></table>
</form>

<jsp:include page="validation_result.jsp"/>

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
