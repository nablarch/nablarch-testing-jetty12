<%@ page pageEncoding="UTF-8" %>
<html>
  <head>
    <title>元気にあいさつ！</title>
  </head>
  <body>
    <%= request.getAttribute("greeting") %>
  </body>
</html>