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

class SynapseClient {
  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  String userId;
  String apiKey;

  private SynapseClient() {}

  public static SynapseClient loginWithAPIKey(final String userId, final String apiKey) {
    final SynapseClient c = new SynapseClient();
    c.userId = userId;
    c.apiKey = apiKey;
    return c;
  }
}

/**
 * Internal exception class used by {@code RestCall}; this ought to be wrapped.
 */
class RestException extends Exception {
  final int code;
  final String msg;
  final String response;

  public RestException(final int code, final String msg, final String response) {
    this.code = code;
    this.msg = msg;
    this.response = response;
  }

  public RestException(final String msg) {
    super(msg);
    this.code = 0;
    this.msg = null;
    this.response = null;
  }

  public RestException(final String msg, final Throwable t) {
    super(msg, t);
    this.code = 0;
    this.msg = null;
    this.response = null;
  }

  public RestException(final IOException e) {
    this("Low-level I/O exception with server", e);
  }

  public String toString() {
    if (msg != null)
      return String.format("%d - %s: %s", code, msg, response);
    else
      return super.toString();
  }
}

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
