#!/usr/bin/python2

# Copyright (c) 2012 by Public Library of Science
# http://plos.org
# http://ambraproject.org
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Author: Ryan Skonnord


"""A minimal library for sending HTTP requests to a RESTful server.

Mostly, this script is a facade to the built-in pycurl library. Its purpose
is to provide a public API that is better suited to making RESTful requests
tersely.
"""

import cStringIO
import httplib
import pycurl
import re
import urllib

# Universal HTTP method names
OPTIONS = 'OPTIONS'
GET     = 'GET'
HEAD    = 'HEAD'
POST    = 'POST'
PUT     = 'PUT'
DELETE  = 'DELETE'
TRACE   = 'TRACE'
CONNECT = 'CONNECT'
PATCH   = 'PATCH'

def status_message(status_code):
    """Translate an HTTP response code to its standard message."""
    try:
        return httplib.responses[status_code]
    except KeyError:
        return '(Undefined)'


class ResponseBuffer(object):
    """An object to provide read/write functions to curl.

    This works better than a bare StringIO because it yields None, rather
    than the empty string, if the write method was never called at all.
    """

    def __init__(self):
        self._buf = None

    def write(self, data):
        """Write to the buffer.

        It is useful to pass this method, as a first-class object, to
        pycurl as a callback.
        """
        if not self._buf:
            self._buf = cStringIO.StringIO()
        self._buf.write(data)

    def read(self):
        """Read what has been written to the buffer.

        If this object has not been written to, return None. Else, return
        all the written data, concatenated. An empty string is returned if
        and only if an empty string was written at least once and no other
        strings were written.
        """
        if self._buf:
            return self._buf.getvalue()
        return None


class Response(object):
    """A response from a RESTful operation."""
    def __init__(self, request, status, body, headers):
        """Construct a response object from response data.

        The headers input can be a raw text blob from curl, a dictionary,
        or a key-value sequence. On construction, it will be parsed into a
        key-value list, which will then be the value of self.headers.
        """
        self.request = request
        self.status = status
        self.body = body
        self.headers = Response._parse_headers(headers)

    _HEADER_SEP = re.compile(r'[\r\n]+')
    _HEADER_GROUPS = re.compile(r'(.*?):\s*(.*)')

    @staticmethod
    def _parse_headers(headers):
        if headers is None:
            # If the response was an empty ResponseBuffer
            return []
        if isinstance(headers, dict):
            # urllib.urlopen provides headers this way
            # There is no ordering of headers to preserve
            return headers.items()
        if getattr(headers, '__iter__', False):
            return list(headers)
        if isinstance(headers, str):
            # curl.setopt(HEADERFUNCTION...) writes a formatted string
            header_items = []
            for item in Response._HEADER_SEP.split(headers):
                if not item:
                    continue
                m = Response._HEADER_GROUPS.match(item)
                header_items.append(m.groups() if m else item)
            return header_items
        msg = "Can't parse headers of type: {0}".format(type(headers))
        raise TypeError(msg)

    def __repr__(self):
        return 'Response({0!r}, {1!r}, {2!r}, {3!r})'.format(
            self.request, self.status, self.body, self.get_headers())

    @staticmethod
    def _display_header_item(header_item):
        """Render one header as a user-readable string.

        The argument is either a simple string value or a (key, value) tuple.
        """
        if isinstance(header_item, str):
            return repr(header_item)
        if len(header_item) != 2:
            raise ValueError("Expected only strings and 2-tuples in headers")
        return '{0!r}: {1!r}'.format(*header_item)

    def display(self, snippet_size=40):
        """Return a user-readable string describing the response."""
        status_description = 'HTTP Status {0}: {1}'.format(
            self.status, status_message(self.status))
        lines = [self.request.get_url(), status_description]

        if not self.headers:
            lines.append('No headers')
        else:
            lines.append('Headers:')
            lines += ('    ' + Response._display_header_item(item)
                      for item in self.headers)
        lines.append('')  # Skip a line before the response body

        if self.body is None:
            lines.append('No response body')
        elif len(self.body) >= snippet_size * 2:
            size = len(self.body)
            head = self.body[ :  snippet_size]
            tail = self.body[-snippet_size : ]
            lines += ['Response size: {0}'  .format(size),
                      'Response head: {0!r}'.format(head),
                      'Response tail: {0!r}'.format(tail)]
        else:
            lines += ['Response body:', repr(self.body)]

        lines.append('\n')  # An extra blank line to separate reports
        return '\n'.join(lines)


class Request(object):
    """A request set up to perform a RESTful operation.

    Sending a request with an instance should not change its state, so
    instances should be reusable if you want the same parameters.

    As of this version, any values passed into this class do not have
    special HTML characters (like '#', '?', and '&') escaped. Bugs may
    ensue if such characters are not manually escaped (like '%23') from
    strings before they are passed in.
    """

    def __init__(self, domain, path, port=None):
        self.domain = domain
        self.port = port
        self.path = path
        self.query_params = dict()
        self.form_params = dict()

    def set_query_parameter(self, key, value):
        """Add a parameter to the URL query.

        If this method is called once or more, the key-value pairs will be
        appended to the end of the URL, as in
            http://example.com/page.html?spam=canned&eggs=fried
        """
        self.query_params[key] = value

    def set_form_parameter(self, key, value):
        """Add the value for an HTML <form> element.

        These values are used only with uploading (POST and PUT)
        operations.
        """
        self.form_params[key] = value

    def set_form_file_path(self, key, file_path):
        """Upload a form file using a local path to the file."""
        self.set_form_parameter(key, (pycurl.FORM_FILE, file_path))

    def get_url(self):
        """Get the full URL that this request will go to."""
        buf = ['http://', self.domain]
        if self.port:
            buf += [':', str(self.port)]
        buf += ['/', self.path]
        in_query = False
        for (query_key, query_value) in self.query_params.items():
            buf += [('&' if in_query else '?'),
                    str(query_key), '=', str(query_value)]
            in_query = True
        return ''.join(buf)

    def _build_curl(self):
        """Build a Curl object to execute this request.

        The returned object is extended with methods named read_body and
        read_headers that will, after perform is called on the same object,
        return the response body and headers respectively.
        """
        curl = pycurl.Curl()
        curl.setopt(pycurl.URL, ''.join(self.get_url()))

        read_body = ResponseBuffer()
        curl.setopt(pycurl.WRITEFUNCTION, read_body.write)
        curl.read_body = read_body.read

        read_headers = ResponseBuffer()
        curl.setopt(pycurl.HEADERFUNCTION, read_headers.write)
        curl.read_headers = read_headers.read

        return curl

    def _send_with_form_data(self, method_opt):
        """Send the request with the form options."""
        curl = self._build_curl()

        curl.setopt(method_opt, 1)
        if self.form_params:
            curl.setopt(pycurl.HTTPPOST, self.form_params.items())

        curl.perform()
        return Response(self, curl.getinfo(pycurl.RESPONSE_CODE),
                        curl.read_body(), curl.read_headers())

    def post(self):
        """Send a POST request."""
        return self._send_with_form_data(pycurl.POST)

    def get(self):
        """Send a GET request."""
        url = self.get_url()
        response = urllib.urlopen(url)
        return Response(self, response.getcode(),
                        response.read(), response.headers.items())

    def put(self):
        """Send a PUT request."""
        return self._send_with_form_data(pycurl.PUT)

    def delete(self):
        """Send a DELETE request."""
        curl = self._build_curl()
        curl.setopt(pycurl.CUSTOMREQUEST, DELETE)
        curl.perform()
        return Response(self, curl.getinfo(pycurl.RESPONSE_CODE),
                        curl.read_body(), curl.read_headers())

    HTTP_METHODS = {
        OPTIONS: None,
        GET:     get,
        HEAD:    None,
        POST:    post,
        PUT:     put,
        DELETE:  delete,
        TRACE:   None,
        CONNECT: None,
        PATCH:   None,
        }
    """HTTP "verbs" mapped onto the methods that handle them."""

    def send(self, method):
        """Send a request.

        The argument is an HTTP "verb" (as an all-uppercase string) that
        appears in Request.HTTP_METHODS.keys(). Only keys with a non-None
        value are supported.
        """
        m = Request.HTTP_METHODS[method]
        if m is None:
            raise ValueError("Method not supported")
        return m(self)
