[![Build Status Badge]][Build Status]

"Rhino" is the nickname for the back-end service component in the Ambra stack.
(The Ambra stack's "Rhino" has no connection with the [JavaScript engine of the
same name](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino).)

Rhino is a loosely RESTful service that exposes a JSON-based API. Articles are
ingested into it by HTTP requests. Then, it serves both the article content and
metadata to other services such as [Wombat](https://github.com/PLOS/wombat).

See the [Ambra Project documentation](https://plos.github.io/ambraproject/) for
an overview of the stack and user instructions. If you have any questions or
comments, please email dev@ambraproject.org, open a [GitHub
issue](https://github.com/PLOS/rhino/issues), or submit a pull request.


[Build Status]: https://teamcity.plos.org/teamcity/viewType.html?buildTypeId=Rhino_Build
[Build Status Badge]: https://teamcity.plos.org/teamcity/app/rest/builds/buildType:(id:Rhino_Build)/statusIcon.svg
