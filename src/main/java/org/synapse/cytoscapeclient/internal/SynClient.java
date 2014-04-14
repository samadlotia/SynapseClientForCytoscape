package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.nio.charset.Charset;

import java.net.URLEncoder;

import java.util.List;
import java.util.ArrayList;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.cytoscape.work.TaskMonitor;

public class SynClient {
  public static class UserProfile {
    final String ownerId;
    final String userName;

    public UserProfile(final String ownerId, final String userName) {
      this.ownerId = ownerId;
      this.userName = userName;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getUserName() {
      return userName;
    }
  }

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

  public static class Entity {
    final String id;
    final String name;
    final EntityType type;

    public Entity(final String id, final String name, final String type) {
      this.id = id;
      this.name = name;
      this.type = EntityType.guess(type);
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public EntityType getType() {
      return type;
    }

    public String toString() {
      return name;
    }
  }

  public static enum EntityType {
    PROJECT("Project", "project"),
    FILE("File", "file"),
    FOLDER("Folder", "folder");

    final String userName;
    final String alias;
    EntityType(final String userName, final String alias) {
      this.userName = userName;
      this.alias = alias;
    }

    public String toString() {
      return userName;
    }

    public String getAlias() {
      return alias;
    }

    public static EntityType guess(final String typeStr) {
      final String typeStrLower = typeStr.toLowerCase();
      for (final EntityType type : values()) {
        final String alias = type.getAlias();
        if (typeStrLower.endsWith(alias) || typeStrLower.endsWith(alias + "entity")) {
          return type;
        }
      }
      return null;
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

  final CloseableHttpClient client;

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
    if (resp == null)
      return null;
    ensureResponse(resp);
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(resp.getEntity().getContent(), JsonNode.class);
    } catch (IOException e) {
      throw new SynClientException("Unable to read response", e);
    }
  }

  private HttpPost newPostJson(final String url, final TreeNode jobj) throws UnsupportedEncodingException, IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final ObjectMapper mapper = new ObjectMapper();
    final JsonFactory jsonFactory = mapper.getFactory();
    final JsonGenerator generator = jsonFactory.createGenerator(output, JsonEncoding.UTF8);
    generator.writeTree(jobj);
    output.flush();
    output.close();
    final ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    final HttpPost post = new HttpPost(url);
    post.setEntity(new InputStreamEntity(input, ContentType.APPLICATION_JSON));
    return post;
  }

  abstract class ReqTask<T> extends ResultTask<T> {
    protected volatile boolean cancelled = false;
    volatile HttpUriRequest req = null;
    volatile CloseableHttpResponse resp = null;

    protected HttpResponse exec(final HttpUriRequest req) throws Exception {
      this.req = req;
      closeResponse();
      try {
        resp = client.execute(req);
        return resp;
      } catch (Exception e) {
        closeResponse();
        if (!cancelled) { // ignore exceptions thrown if cancelled
          throw e;
        }
      } finally {
        this.req = null;
      }
      return null;
    }

    protected void closeResponse() {
      if (resp == null)
        return;
      try {
        resp.close();
      } catch (IOException e) {}
      resp = null;
    }

    public void cancel() {
      cancelled = true; // this MUST be set before calling HttpUriRequest.abort()
      final HttpUriRequest req2 = this.req; // copy the reference to req to prevent it from becoming null while trying to abort it
      if (req2 != null) {
        req2.abort();
      }
      closeResponse();
    }
  }

  public ResultTask<UserProfile> newUserProfileTask() {
    return new ReqTask<UserProfile>() {
      protected UserProfile checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get Synapse user profile");
        final HttpResponse resp = super.exec(new HttpGet(join(REPO_ENDPOINT, "/userProfile/")));
        if (resp == null)
          return null;
        final JsonNode root = toJson(resp);
        closeResponse();
        return new UserProfile(root.get("ownerId").asText(), root.get("userName").asText());
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

  public ResultTask<SynFile> newFileTask(final String entityId) {
    return new ReqTask<SynFile>() {
      protected SynFile checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Download file " + entityId);
        monitor.setProgress(-1);

        // get info about the entity
        monitor.setStatusMessage("Retrieving entity info");
        final JsonNode entityInfo = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/bundle?mask=1"))));
        if (entityInfo == null)
          return null;
        closeResponse();

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
        if (resp == null)
          return null;
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
        closeResponse();

        return new SynFile(file, filename);
      }
    };
  }

  public ResultTask<List<Entity>> newProjectsTask(final UserProfile userProfile) {
    return new ReqTask<List<Entity>>() {
      protected List<Entity> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get projects created by " + userProfile.getUserName());
        final String query = join("SELECT id, name, concreteType FROM project WHERE project.createdByPrincipalId == ", userProfile.getOwnerId());
        final JsonNode jroot = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/query?", query("query", query)))));
        if (jroot == null)
          return null;
        closeResponse();
        final JsonNode jprojects = jroot.get("results");
        final List<Entity> projects = new ArrayList<Entity>();
        for (final JsonNode jproject : jprojects) {
          final String id = jproject.get("project.id").textValue();
          final String name = jproject.get("project.name").textValue();
          final String type = jproject.get("project.concreteType").get(0).textValue();
          final Entity project = new Entity(id, name, type);
          projects.add(project);
        }
        return projects;
      }
    };
  }

  public ResultTask<List<Entity>> newChildrenTask(final String parentId) {
    return new ReqTask<List<Entity>>() {
      protected List<Entity> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get child entities of " + parentId);
        final List<Entity> children = new ArrayList<Entity>();
        final JsonNode jchildren = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", parentId, "/children"))));
        if (jchildren == null)
          return null;
        closeResponse();
        for (final JsonNode jchild : jchildren.get("idList")) {
          if (super.cancelled)
            return null;
          final String id = jchild.get("id").textValue();
          final JsonNode jinfo = toJson(super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", id, "/bundle?mask=", Integer.toString(0x1 | 0x20)))));
          if (jinfo == null)
            return null;
          closeResponse();
          final JsonNode jentity = jinfo.get("entity");
          final String name = jentity.get("name").textValue();
          final String type = jentity.get("entityType").textValue();
          final Entity child = new Entity(id, name, type);
          children.add(child);
        }
        return children;
      }
    };
  }

  public ResultTask<String> newDescriptionIdTask(final String entityId) {
    return new ReqTask<String>() {
      protected String checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get description info of " + entityId);
        final HttpResponse response = super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/wiki2")));
        if (response == null)
          return null;
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 404) {
          closeResponse();
          return null;
        }
        final JsonNode jinfo = toJson(response);
        closeResponse();
        return jinfo.get("id").textValue();
      }
    };
  }

  public ResultTask<String> newDescriptionMarkdownTask(final String entityId, final String descriptionId) {
    return new ReqTask<String>() {
      protected String checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get description of " + entityId);
        final HttpResponse response = super.exec(new HttpGet(join(REPO_ENDPOINT, "/entity/", entityId, "/wiki2/", descriptionId, "/markdown")));
        if (response == null)
          return null;
        final HttpEntity entity = response.getEntity();
        final InputStream input = new GZIPInputStream(entity.getContent());
        final long totalLen = entity.getContentLength();
        ByteArrayOutputStream output = null;
        if (totalLen > 0L) {
          output = new ByteArrayOutputStream((int) totalLen);
        } else {
          output = new ByteArrayOutputStream();
        }

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
        input.close();

        final String encoding = entity.getContentEncoding() == null ? "UTF-8" : entity.getContentEncoding().getValue();
        closeResponse();
        return new String(output.toByteArray(), Charset.forName(encoding));
      }
    };
  }

  public ResultTask<List<Entity>> newSearchTask(final String queryTerm, final EntityType entityType) {
    return new ReqTask<List<Entity>>() {
      protected List<Entity> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Search for " + queryTerm);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNodeFactory jsonNodeFactory = mapper.getNodeFactory();
        // Jackson doesn't support proper chaining when adding fields with JsonNodes as values :(
        final ObjectNode jquery = jsonNodeFactory.objectNode();
        jquery.set("queryTerm",    jsonNodeFactory.arrayNode().add(queryTerm));
        jquery.set("returnFields", jsonNodeFactory.arrayNode().add("name").add("id").add("node_type_r"));
        if (entityType != null) {
          final ObjectNode jtype = jsonNodeFactory.objectNode();
          jtype.put("key",  "node_type");
          jtype.put("value", entityType.getAlias());
          jquery.set("booleanQuery", jsonNodeFactory.arrayNode().add(jtype));
        }
        final JsonNode jresults = toJson(super.exec(newPostJson(join(REPO_ENDPOINT, "/search"), jquery)));
        closeResponse();

        final List<Entity> entities = new ArrayList<Entity>();
        for (final JsonNode jhit : jresults.get("hits")) {
          entities.add(new Entity(jhit.get("id").textValue(),
                                  jhit.get("name").textValue(),
                                  jhit.get("node_type").textValue()));
        }
        return entities;
      }
    };
  }
}