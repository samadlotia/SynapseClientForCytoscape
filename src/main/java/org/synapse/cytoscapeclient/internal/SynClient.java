package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.net.URLEncoder;

import java.util.List;
import java.util.ArrayList;

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
    final File file;
    final String name;

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

  public static class Project {
    final String name;
    final String id;

    public Project(final String name, final String id) {
      this.name = name;
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public String getId() {
      return id;
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

  private static String query(String ... kvs) {
    final int len = kvs.length;
    final StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < len; i += 2) {
      final String k = kvs[i];
      final String v = kvs[i + 1];
      buffer.append(URLEncoder.encode(k));
      buffer.append('=');
      buffer.append(URLEncoder.encode(v));
      if (i < (len - 2)) {
        buffer.append('&');
      }
    }
    return buffer.toString();
  }

  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  final HttpClient client;
  String ownerId = null;

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

  abstract class ReqTask<T> extends MaybeTask<T> {
    volatile HttpUriRequest req = null;

    protected HttpResponse exec(final HttpUriRequest req) throws IOException {
      this.req = req;
      final HttpResponse resp = client.execute(req);
      this.req = null;
      return resp;
    }

    protected void innerCancel() {
      final HttpUriRequest req2 = this.req; // copy the reference to req to prevent it from becoming null while trying to abort it
      if (req2 != null) {
        req2.abort();
      }
    }
  }

  public MaybeTask<String> newGetOwnerTask() {
    return new ReqTask<String>() {
      protected String checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setStatusMessage("Attempting to retrieve login credentials");
        final HttpResponse resp = super.exec(new HttpGet(join(REPO_ENDPOINT, "/userProfile/")));
        final JsonNode root = toJson(resp);
        ownerId = root.get("ownerId").asText();
        return ownerId;
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
    return new ReqTask<SynFile>() {
      protected SynFile checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setProgress(-1);

        // get info about the entity
        monitor.setStatusMessage("Retrieving entity info");
        final JsonNode entityInfo = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/bundle?mask=1"))));

        // ensure that it's a file
        final String entityType = entityInfo.get("entityType").asText();
        if (!entityType.endsWith("FileEntity"))
          throw new SynClientException("Synapse entity ID does not refer to a file: " + entityId);

        // get name and version
        final String filename = entityInfo.get("entity").get("name").asText();
        final String version = entityInfo.get("entity").get("versionLabel").asText();

        // request the file itself
        monitor.setStatusMessage("Downloading file");
        final HttpResponse resp = super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/version/", version, "/file")));
        ensureResponse(resp);

        // download the file to a temp
        final File file = newTempFile(filename);
        final InputStream input = resp.getEntity().getContent();
        final long totalLen = resp.getEntity().getContentLength();
        final FileOutputStream output = new FileOutputStream(file);
        final byte[] buffer = new byte[BLOCK_SIZE];
        long readLen = 0;
        while (!super.cancelled) {
          final int len = input.read(buffer);
          if (len < 0)
            break;
          output.write(buffer, 0, len);

          readLen += len;
          if (totalLen >= 0L) {
            monitor.setProgress(((double) readLen) / ((double) totalLen));
          }
        }
        output.close();
        input.close();

        return new SynFile(file, filename);
      }
    };
  }

  public MaybeTask<List<Project>> newProjectsTask() {
    return new ReqTask<List<Project>>() {
      protected List<Project> checkedRun(final TaskMonitor monitor) throws Exception {
        final JsonNode jroot = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/query?", query("query", join("SELECT * FROM project WHERE project.createdByPrincipalId == ", ownerId))))));
        System.out.println("/query response: " + jroot.toString());
        final JsonNode jprojects = jroot.get("results");
        final List<Project> projects = new ArrayList<Project>();
        for (final JsonNode jproject : jprojects) {
          final Project project = new Project(jproject.get("project.name").textValue(), jproject.get("project.id").textValue());
          projects.add(project);
        }
        return projects;
      }
    };
  }
}