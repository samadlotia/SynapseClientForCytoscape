package org.synapse.cytoscapeclient.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wrapper for an {@code HttpURLConnection} that makes it easier
 * to issue rest API calls by using the builder pattern.
 */
class RestCall {
  final HttpURLConnection connection;

  private RestCall(String url) throws MalformedURLException, IOException {
    connection = (HttpURLConnection) (new URL(url)).openConnection();
    connection.setUseCaches(false);
  }

  /**
   * Start a Rest call to the given URL.
   */
  public static RestCall to(final String urlStr) throws RestException {
    try {
      return new RestCall(urlStr);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("URL is not valid: " + urlStr, e);
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  /**
   * Return the underlying connection for this rest call.
   */
  public HttpURLConnection getConnection() {
    return connection;
  }

  /**
   * Set an http request header.
   */
  public RestCall header(final String k, final String v) {
    connection.setRequestProperty(k, v);
    return this;
  }

  /**
   * Set the http method to GET, PUSH, etc.
   */
  public RestCall method(String method) {
    try {
      connection.setRequestMethod(method);
    } catch (ProtocolException e) {
      throw new IllegalArgumentException("Unrecognized method: " + method, e);
    }
    return this;
  }

  /**
   * Complete the rest call but ignore anything the server sends back.
   * @throws RestException failure to communicate with server or unsuccessful http request, i.e. non 2xx response
   */
  public void done() throws RestException {
    try {
      checkResponse();
      connection.disconnect();
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  /**
   * Complete the rest call and return what the server sent back as a string.
   */
  public String asText() throws RestException {
    try {
      checkResponse();
      final String result = slurp(connection.getInputStream());
      connection.disconnect();
      return result;
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  /**
   * Complete the rest call and return what the server sent back as parsed JSON.
   */
  public JsonNode asJson() throws RestException {
    try {
      checkResponse();
      final InputStream input = connection.getInputStream();
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode root = mapper.readValue(input, JsonNode.class);
      connection.disconnect();
      return root;
    } catch (IOException e) {
      throw new RestException(e);
    }
  }

  private void checkResponse() throws RestException, IOException {
    connection.connect();
    final int code = connection.getResponseCode();
    final String msg = connection.getResponseMessage();

    if (200 <= code && code < 300) {
      return; // success!
    }

    final String error = slurp(connection.getErrorStream());
    throw new RestException(code, msg, error);
  }

  private static String slurp(final InputStream stream) {
    final Scanner scanner = new Scanner(stream).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}