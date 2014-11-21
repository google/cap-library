<%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page import="com.google.publicalerts.cap.validator.StringUtil"
%><%@ page import="java.util.List"
%><!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta http-equiv="content-language" content="en">
<title>Subscribe a feed to the CAP Validator</title>
<link href="/style.css" type="text/css" rel="stylesheet">
</head>
<body>
<div class=header>
  <img src="/googleorg_logo.gif" alt="Google Labs" class="logo">
  <span class=title>Common Alerting Protocol Validator</span>
</div>
<div style="margin:3em 0">

Subscribe to automatically validate your CAP alerts.

<ol>
<li>Enter the URL of your Atom, RSS or and EDXL-DE CAP feed.
<li>Enter an email address.
<li>Optionally choose CAP profiles to validate against.
<li>Subscribe
<li>You receive an email any time there is an error in your feed of CAP alerts.
</ol>
</div><%

String msg = (String) request.getAttribute("msg");
if (msg != null) {%>
  <div class=banner><%= msg %></div>
<%}
%><form id=inputform action="/subscribe" method=post>
  <table cellpadding=0 cellspacing=0 style="width: 800px">
    <tr>
      <td>Feed URL</td>
      <td><input type=text name="topic" value="<%= StringUtil.htmlEscape((String) request.getAttribute("topic")) %>" size=50/>
          <span class=def>Example: https://alerts.weather.gov/cap/us.php?x=0</span></td>
    </tr>
    <tr>
      <td>Email</td>
      <td><input type=text name="email" value="<%= StringUtil.htmlEscape((String) request.getAttribute("email")) %>" size=50/>
          <span class=def>Example: user1@example1.com,user2@example2.com</span></td>
    </tr>
    <tr>
      <td>CAP profiles</td>
      <td><%
    List<String[]> profiles = (List<String[]>) request.getAttribute("profiles");
    if (profiles != null) {
      %><div>
        <div>
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
    %></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
      <br/>
      <input type=submit class=submit name=submit value="Subscribe"/>&nbsp;
      <input type=submit class=submit name=submit value="Unsubscribe"/></td>
    </tr>
  </table>
</form>

<div class=footer>
  <p class="homeabout">&#xa9;2011 Google - <a href="http://www.google.com/accounts/TOS">Terms of Service</a> - <a href="https://github.com/google/cap-library">About the Common Alerting Protocol Validator</a> - <a href="http://www.google.com/intl/en/privacy.html">Privacy Policy</a></p>
  <img src="http://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine" height="30" width="120"/>
</div>

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
