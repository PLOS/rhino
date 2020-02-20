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

Rhino is configured using the following environment variables:

- `PRETTY_PRINT_JSON`: optional, set to `true` to pretty print JSON in API responses
- `DATABASE_URL`: how to connect to the MYSQL database. Form: `jdbc:mysql://HOSTNAME:3306/DATABASE?user=USERNAME&password=PASSWORD`
- `CORPUS_BUCKET`: name of the corpus bucket
- `EDITORIAL_BUCKET`: optional, name of the editorial bucket
- `CONTENT_REPO_URL`: URL for the contentrepo instance to use, e.g. `http://localhost:8002/v1/`
- `TAXONOMY_URL`: URL of the access innovations server to use, e.g. `https://localhost:9138/servlet/dh`
- `THESAURUS`: Name of the thesaurus to use, e.g. `plosthes.2017-2`

Full example of running using `cargo`:
```
$ CORPUS_BUCKET=mogilefs-prod-repo CONTENT_REPO_URL=http://localhost:8002/v1/ TAXONOMY_URL=https://localhost:9138/servlet/dh THESAURUS=plosthes.2017-2 DATABASE_URL="jdbc:mysql://localhost:3306/ambra?user=root&password=password" mvn package cargo:run`
```

[Build Status]: https://teamcity.plos.org/teamcity/viewType.html?buildTypeId=Rhino_Build
[Build Status Badge]: https://teamcity.plos.org/teamcity/app/rest/builds/buildType:(id:Rhino_Build)/statusIcon.svg
