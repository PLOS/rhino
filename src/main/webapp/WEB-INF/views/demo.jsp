<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page session="false" %>
<%--
  ~ Copyright (c) 2006-2012 by Public Library of Science
  ~ http://plos.org
  ~ http://ambraproject.org
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<html>
<head>
  <title>Ambra Admin TNG: Demo Page</title>
</head>
<body>
<p>Welcome to the <b>Ambra Admin "TNG"</b> prototype. Your locale is ${clientLocale}. Server time is ${serverTime}.</p>

<p>There are ${articleCount} articles in the database.</p>

<p>List of article DOIs:</p>
<ol>
  <c:forEach var="doi" items="${articleDoiList}">
    <li>${doi}</li>
  </c:forEach>
</ol>
</body>
</html>
