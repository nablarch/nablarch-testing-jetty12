<%
    String style_css  = response.encodeURL("./style.css");
    String common_css = response.encodeURL("/css/common.css"); 
    String js_url     = response.encodeURL("/js/common.js"); 
    String relative_css01  = response.encodeURL("relative.css");
    String relative_css02  = response.encodeURL("hoge/relative.css");
    String relative_css03  = response.encodeURL("../hoge/relative.css");
    String relative_css04  = response.encodeURL("../../hoge/relative.css");
%>

<html>
<head>
  <LINK REL="stylesheet" TYPE="text/css" HREF="<%=common_css%>" />
  <link rel="stylesheet" type="text/css" href="<%=style_css%>" />
  <script language="javascript" src="<%=js_url%>" />
  <LINK REL="stylesheet" TYPE="text/css" HREF="<%=relative_css01%>" />
  <LINK REL="stylesheet" TYPE="text/css" HREF="<%=relative_css02%>" />
  <LINK REL="stylesheet" TYPE="text/css" HREF="<%=relative_css03%>" />
  <LINK REL="stylesheet" TYPE="text/css" HREF="<%=relative_css04%>" />
</head>
<body>
  <p>Hello world</p>
  <p>
    <a href="/page/example.jsp">example</a>
  </p>
</body>
</html>