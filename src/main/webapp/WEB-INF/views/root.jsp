<%--
  Copyright (c) 2013 by Public Library of Science
  http://plos.org
  http://ambraproject.org

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
  <title>Ambra Service REST API</title>
</head>
<body>

<p>This is the root page of the REST API for Ambra services.</p>

<ul>
  <li><a href="articles">/articles</a>
    <ul>
      <li>
        ?state=
        <c:forEach items="${stateParams}" var="stateParam">
          <a href="articles?state=${stateParam}">${stateParam}</a>
        </c:forEach>
      </li>
      <li>
        ?syndication=
        <c:forEach items="${syndStatuses}" var="syndStatus">
          <a href="articles?syndication=${syndStatus}">${syndStatus}</a>
        </c:forEach>
      </li>
      <li><a href="articles?pingbacks">?pingbacks</a></li>
    </ul>
  </li>
  <li><a href="ingestibles">/ingestibles</a></li>
  <li><a href="config">/config</a></li>
</ul>

<hr/>
<div><img src="resources/rhino.jpg" alt="Rhino relaxation"/></div>

<div style="font-size: small">
  Image: <a href="http://www.flickr.com/photos/macinate/2810203599/">"Rhino relaxation"</a> by
  <a href="http://www.flickr.com/photos/macinate/">macinate</a>.
  Available under the
  <a href="http://creativecommons.org/licenses/by/2.0/deed.en">Creative Commons Attribution 2.0 Generic License.</a>
</div>

</body>
</html>
