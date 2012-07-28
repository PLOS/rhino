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
"""

import cStringIO
import pycurl
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

class ResponseReceiver:
    """An object to provide read/write functions.

    This works better than a bare StringIO because it yields None, rather than
    the empty string, if the write method was never called at all.
    """

    def __init__(self):
        self._buf = None

    def write(self, data):
        """Write to the buffer.

        This method is useful for passing to pycurl as a callback.
        """
        if not self._buf:
            self._buf = cStringIO.StringIO()
        self._buf.write(data)

    def read(self):
        """Read what has been written to the buffer.

        If this object has not been written to, return None. Else, return all
        the written data, concatenated. The empty string is returned if and
        only if an empty string was written at least once and no other strings
        were.
        """
        if self._buf:
            return self._buf.getvalue()
        return None

class Request:

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
        self.quary_params[key] = value

    def set_form_parameter(self, key, value):
        """Add the value for an HTML <form> element.

        These values are used only with uploading (POST and PUT) operations.
        """
        self.form_params[key] = value

    def set_form_file_path(self, key, file_path):
        """Upload a form file using a local path to the file.
        """
        self.set_form_parameter(key, (pycurl.FORM_FILE, file_path))

    def _build_url(self):
        """Build the URL that this request will go to."""
        buf = ['http://', self.domain]
        if self.port:
            buf += [':' + str(self.port)]
        buf += ['/', self.path]
        in_query = False
        for (query_key, query_value) in self.query_params.items():
            buf += [('&' if in_query else '?'),
                    query_key, '=', query_value]
            in_query = True
        return ''.join(buf)

    def _build_curl(self):
        """Build a Curl object to execute this request.

        The returned object is extended with a method (actually just a member
        function) named read_response that will, after perform is called on the
        same object, return the response body.
        """
        curl = pycurl.Curl()
        curl.setopt(pycurl.URL, ''.join(self._build_url()))

        rr = ResponseReceiver()
        curl.setopt(pycurl.WRITEFUNCTION, rr.write)
        curl.read_response = rr.read
        return curl

    def _send_with_form_data(self, method_opt):
        """Send the request with the form options."""
        curl = self._build_curl()

        curl.setopt(method_opt, 1)
        if self.form_params:
            curl.setopt(pycurl.HTTPPOST, self.form_params.items())

        curl.perform()
        return curl.getinfo(pycurl.RESPONSE_CODE), curl.read_response()

    def post(self):
        """Send a POST request."""
        return self._send_with_form_data(pycurl.POST)

    def get(self):
        """Send a GET request."""
        url = self._build_url()
        response = urllib.urlopen(url)
        return response.getcode(), response.read()

    def put(self):
        """Send a PUT request."""
        return self._send_with_form_data(pycurl.PUT)

    def delete(self):
        """Send a DELETE request."""
        curl = self._build_curl()
        curl.setopt(pycurl.CUSTOMREQUEST, 'DELETE')
        curl.perform()
        return curl.getinfo(pycurl.RESPONSE_CODE), curl.read_response()

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

    def send(self, method):
        """Send a request.

        The argument is an HTTP "verb".
        """
        m = Request.HTTP_METHODS[method]
        if m is None:
            raise Exception("Method not supported")
        return m(self)
