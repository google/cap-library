<%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page import="com.google.publicalerts.cap.validator.StringUtil"
%><%@ page import="java.util.List"
%>
<p><a href="<%=StringUtil.htmlEscape((String) request.getAttribute("url"))%>">http://cap-validator.appspot.com</a> found problems with CAP alerts you published recently.

<p>You can <a href="<%=StringUtil.htmlEscape((String) request.getAttribute("unsubscribe"))%>">unsubscribe</a> from these notifications at any time.

<jsp:include page="validation_result.jsp"/>
