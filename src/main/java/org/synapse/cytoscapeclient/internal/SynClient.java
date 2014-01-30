package org.synapse.cytoscapeclient.internal.nau;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.cytoscape.work.TaskMonitor;

public class SynClient {
  public static class SynFile {
    public final File file;
    public final String name;

    public SynFile(File file, String name) {
      this.file = file;
      this.name = name;
    }

    public File getFile() {
      return file;
    }

    public String getName() {
      return name;
    }
  }

  private static String join(String ... pieces) {
    final int len = pieces.length;
    final StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < len; i++) {
      buffer.append(pieces[i]);
    }
    return buffer.toString();
  }

  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  final HttpClient client;

  public SynClient(final HttpRequestInterceptor auth) {
    client = 
      HttpClientBuilder.create()
        .addInterceptorLast(auth)
        .build();
  }

  private void ensureResponse(final HttpResponse resp) throws SynClientException {
    final int statusCode = resp.getStatusLine().getStatusCode();
    if (!(200 <= statusCode && statusCode < 300)) {
      throw new SynClientException("Request failed: " + resp.getStatusLine());
    }
  }

  private JsonNode toJson(final HttpResponse resp) throws SynClientException {
    ensureResponse(resp);
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(resp.getEntity().getContent(), JsonNode.class);
    } catch (IOException e) {
      throw new SynClientException("Unable to read response", e);
    }
  }

  public MaybeTask<String> newGetOwnerTask() {
    return new MaybeTask<String>() {
      HttpUriRequest req = null;

      protected String checkedRun(final TaskMonitor monitor) throws Exception {
        req = new HttpGet(join(REPO_ENDPOINT, "/userProfile/"));
        final HttpResponse resp = client.execute(req);
        final JsonNode root = toJson(resp);
        return root.get("ownerId").asText();
      }

      protected void innerCancel() {
        if (req != null) {
          req.abort();
        }
      }
    };
  }

  static File newTempFile(final String fullname) throws IOException {
    final int exti = fullname.lastIndexOf('.');
    if (exti < 0) {
      return File.createTempFile(fullname, null);
    } else {
      final String name = fullname.substring(0, exti);
      final String ext = fullname.substring(exti);
      return File.createTempFile(name, ext);
    }
  }

  static final int BLOCK_SIZE = 64 * 1024;

  public MaybeTask<SynFile> newGetFileTask(final String entityId) {
    return new MaybeTask<SynFile>() {
      HttpUriRequest req = null;

      protected SynFile checkedRun(final TaskMonitor monitor) throws Exception {
        // get info about the entity
        req = new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/bundle?mask=1"));
        final JsonNode entityInfo = toJson(client.execute(req));

        // ensure that it's a file
        final String entityType = entityInfo.get("entityType").asText();
        if (!entityType.endsWith("FileEntity"))
          throw new SynClientException("Synapse entity ID does not refer to a file: " + entityId);

        // get name and version
        final String filename = entityInfo.get("entity").get("name").asText();
        final String version = entityInfo.get("entity").get("versionLabel").asText();

        // request the file itself
        req = new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/version/", version, "/file"));
        final HttpResponse resp = client.execute(req);
        ensureResponse(resp);

        // download the file to a temp
        final File file = newTempFile(filename);
        final InputStream input = resp.getEntity().getContent();
        final FileOutputStream output = new FileOutputStream(file);
        final byte[] buffer = new byte[BLOCK_SIZE];
        while (!super.cancelled) {
          final int len = input.read(buffer);
          if (len < 0)
            break;
          output.write(buffer, 0, len);
        }
        output.close();
        input.close();

        return new SynFile(file, filename);
      }

      protected void innerCancel() {
        if (req != null) {
          req.abort();
        }
      }
    };
  }
}