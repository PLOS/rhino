package org.ambraproject.rhino.util.response;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.rhombat.HttpDateUtil;
import org.apache.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TransceiverTest {

  private static class TestTransceiver extends Transceiver {
    private final Calendar lastModifiedDate;
    private final Object data;
    private boolean hasGottenData;

    private TestTransceiver(Calendar lastModifiedDate) {
      this(lastModifiedDate, ImmutableMap.of());
    }

    private TestTransceiver(Calendar lastModifiedDate, Object data) {
      this.lastModifiedDate = lastModifiedDate;
      this.data = data;
      this.hasGottenData = false;
    }

    @Override
    public Calendar getLastModifiedDate() {
      return lastModifiedDate;
    }

    @Override
    protected Object getData() throws IOException {
      hasGottenData = true;
      return data;
    }

    public boolean hasGottenData() {
      return hasGottenData;
    }
  }

  @Test
  public void testRespondWithEqualDate() throws Exception {
    Calendar date = HttpDateUtil.parse("Sat, 22 Jan 2011 08:01:00 GMT");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, HttpDateUtil.format(date));

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setWriterAccessAllowed(false);

    TestTransceiver transceiver = new TestTransceiver(date);
    transceiver.respond(request, response, new Gson());
    assertFalse(transceiver.hasGottenData());
  }

  @Test
  public void testRespondWithAfterDate() throws Exception {
    Calendar ifSince = HttpDateUtil.parse("Sat, 22 Jan 2011 08:01:00 GMT");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, HttpDateUtil.format(ifSince));

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setWriterAccessAllowed(false);

    Calendar modified = HttpDateUtil.parse("Fri, 21 Jan 2011 08:01:00 GMT");
    TestTransceiver transceiver = new TestTransceiver(modified);
    transceiver.respond(request, response, new Gson());
    assertFalse(transceiver.hasGottenData());
  }

  @Test
  public void testRespondWithModified() throws Exception {
    Calendar ifSince = HttpDateUtil.parse("Sat, 22 Jan 2011 08:01:00 GMT");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, HttpDateUtil.format(ifSince));

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setWriterAccessAllowed(true);

    Calendar modified = HttpDateUtil.parse("Sun, 23 Jan 2011 08:01:00 GMT");
    TestTransceiver transceiver = new TestTransceiver(modified);
    transceiver.respond(request, response, new Gson());
    assertTrue(transceiver.hasGottenData());
  }

  /**
   * Dummy class to simulate a Gson serialization error.
   */
  private static class BadSerializationObject {
  }

  private static class MockJsonSerializationFailure extends RuntimeException {
  }

  @Test
  public void testGracefulGsonFailure() throws Exception {
    Calendar modified = HttpDateUtil.parse("Sat, 22 Jan 2011 08:01:00 GMT");
    TestTransceiver transceiver = new TestTransceiver(modified, new BadSerializationObject()) {
      @Override
      protected boolean bufferResponseBody() {
        return true;
      }
    };

    // Set Gson up to fail if it attempts to serialize a BadSerializationObject
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(BadSerializationObject.class, new JsonSerializer<BadSerializationObject>() {
      @Override
      public JsonElement serialize(BadSerializationObject src, Type typeOfSrc, JsonSerializationContext context) {
        throw new MockJsonSerializationFailure();
      }
    });
    Gson gson = gsonBuilder.create();

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setWriterAccessAllowed(false); // This is the important part -- test that the exception prevents writing
    try {
      transceiver.respond(new MockHttpServletRequest(), response, gson);
      fail(); // Expected an exception
    } catch (MockJsonSerializationFailure e) {
      // Expected
    }
    assertTrue(transceiver.hasGottenData());
    assertEquals(response.getContentAsByteArray().length, 0);
  }

  @Test
  public void testJsonpResponse() throws Exception {
    Calendar modified = HttpDateUtil.parse("Sat, 22 Jan 2011 08:01:00 GMT");
    Object testData = ImmutableMap.of();
    TestTransceiver transceiver = new TestTransceiver(modified, testData);

    MockHttpServletRequest request = new MockHttpServletRequest();
    String callbackParam = "myTestingCallback";
    request.addParameter("callback", callbackParam);
    MockHttpServletResponse response = new MockHttpServletResponse();

    Gson gson = new Gson();
    transceiver.respond(request, response, gson);

    String actualContent = response.getContentAsString();
    String expectedContent = String.format("%s(%s)", callbackParam, gson.toJson(testData));
    assertEquals(actualContent.trim(), expectedContent.trim());
  }

}
