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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.client.protocol.HttpClientContext;

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
  final PoolingHttpClientConnectionManager connMgr;

  public SynClient(final HttpRequestInterceptor auth) {
    /*
    final RequestConfig conf =
      RequestConfig.custom()
      .setConnectTimeout(10*1000)
      .setStaleConnectionCheckEnabled(true)
      .build();
    */
    connMgr = new PoolingHttpClientConnectionManager();
    connMgr.setDefaultMaxPerRoute(AsyncTaskMgr.N_THREADS);
    client = 
      HttpClientBuilder.create()
        .addInterceptorLast(auth)
        .setConnectionManager(connMgr)
        //.setDefaultRequestConfig(conf)
        .build();
  }

  abstract class ReqTask<T> extends ResultTask<T> {
    protected final HttpContext context;
    protected volatile boolean cancelled = false;
    volatile HttpRequestBase req = null;
    volatile CloseableHttpResponse resp = null;

    public ReqTask() {
      this.context = HttpClientContext.create();
    }

    protected ReqTask<T> get(final String ... pieces) throws ClientProtocolException, IOException {
      this.req = new HttpGet(join(pieces));
      return exec();
    }

    protected ReqTask<T> postJson(final String url, final TreeNode jobj) throws UnsupportedEncodingException, ClientProtocolException, IOException {
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
      this.req = post;
      return exec();
    }

    private ReqTask<T> exec() throws IOException, ClientProtocolException {
      try {
        System.out.println(String.format("Req[%x] exec(%s)", this.hashCode(), req));
        this.resp = client.execute(req, context);
      } catch (IOException e) {
        if (resp != null) {
          end(); // clean up req & response obj
        }
        if (!cancelled) { // ignore exceptions thrown if cancelled
          System.out.println("Req failed: " + req);
          throw e;
        }
      }
      return this;
    }

    protected ReqTask<T> ensure2xx() throws SynClientException {
      if (resp == null)
        return this;
      final int statusCode = resp.getStatusLine().getStatusCode();
      if (!(200 <= statusCode && statusCode < 300)) {
        final String reason = resp.getStatusLine().getReasonPhrase();
        end();
        throw new SynClientException(statusCode + " " + reason);
      }
      return this;
    }

    protected JsonNode json() throws IOException {
      if (resp == null)
        return null;
      final ObjectMapper mapper = new ObjectMapper();
      try {
        return mapper.readValue(resp.getEntity().getContent(), JsonNode.class);
      } finally {
        end();
      }
    }

    protected String string() throws IOException {
      if (resp == null)
        return null;
      final HttpEntity entity = resp.getEntity();
      final String contentType = entity.getContentType() == null ? null : entity.getContentType().getValue();
      final String charset = entity.getContentEncoding() == null ? null : entity.getContentEncoding().getValue();
      InputStream input = entity.getContent();
      try {
        if (contentType != null && contentType.equals("application/x-gzip")) {
          input = new GZIPInputStream(input);
        }
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        while (!cancelled) {
          final int len = input.read(buffer);
          if (len < 0)
            break;
          output.write(buffer, 0, len);
        }
        return new String(output.toByteArray(), Charset.forName(charset == null ? "UTF-8" : charset));
      } finally {
        try { input.close(); } catch (IOException e) {}
        end();
      }
    }

    static final int BLOCK_SIZE = 256 * 1024; /* 256 kb per block */

    protected void write(final OutputStream output, final TaskMonitor monitor) throws IOException {
      if (resp == null)
        return;
      final InputStream input = resp.getEntity().getContent();
      try {
        final long totalLen = resp.getEntity().getContentLength();
        final byte[] buffer = new byte[BLOCK_SIZE];
        long readLen = 0;
        while (!cancelled) {
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
        output.close();
      } finally {
        end();
      }
    }

    protected void end() {
      System.out.println(String.format("Req[%x] end()", this.hashCode(), req));
      try {
        resp.close();
        req.releaseConnection();
      } catch (IOException e) {
      } finally {
        req = null;
        resp = null;
        connMgr.closeExpiredConnections();
      }
    }

    public void cancel() {
      cancelled = true; // this MUST be set before calling HttpUriRequest.abort() to avoid unnecessary exceptions being thrown
      final HttpUriRequest req2 = this.req; // copy the reference to req to prevent it from becoming null while trying to abort it
      if (req2 != null) {
        req2.abort();
      }
      if (resp != null) {
        end();
      }
    }
  }

  public ResultTask<UserProfile> newUserProfileTask() {
    return new ReqTask<UserProfile>() {
      protected UserProfile checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get Synapse user profile");
        final JsonNode jroot = get(REPO_ENDPOINT, "/userProfile/").ensure2xx().json();
        if (jroot == null)
          return null;
        return new UserProfile(jroot.get("ownerId").asText(), jroot.get("userName").asText());
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

  public ResultTask<SynFile> newFileTask(final String entityId) {
    return new ReqTask<SynFile>() {
      protected SynFile checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Download file " + entityId);
        monitor.setProgress(-1);

        // get info about the entity
        monitor.setStatusMessage("Retrieving entity info");
        final JsonNode jentityInfo = get(REPO_ENDPOINT, "/entity/", entityId, "/bundle?mask=1").ensure2xx().json();
        if (jentityInfo == null)
          return null;

        // ensure that it's a file
        final String entityType = jentityInfo.get("entityType").asText();
        if (!EntityType.FILE.equals(EntityType.guess(entityType)))
          throw new SynClientException("Synapse entity ID does not refer to a file: " + entityId);

        // get name and version
        final String filename = jentityInfo.get("entity").get("name").asText();
        final String version = jentityInfo.get("entity").get("versionLabel").asText();

        // request the file itself
        monitor.setStatusMessage("Downloading file");
        final File file = newTempFile(filename);
        final FileOutputStream output = new FileOutputStream(file);
        get(join(REPO_ENDPOINT, "/entity/", entityId, "/version/", version, "/file")).ensure2xx().write(output, monitor);

        return new SynFile(file, filename);
      }
    };
  }

  public ResultTask<List<Entity>> newProjectsTask(final UserProfile userProfile) {
    return new ReqTask<List<Entity>>() {
      protected List<Entity> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get projects created by " + userProfile.getUserName());
        final String query = join("SELECT id, name, concreteType FROM project WHERE project.createdByPrincipalId == ", userProfile.getOwnerId());
        final JsonNode jroot = get(REPO_ENDPOINT, "/query?", query("query", query)).ensure2xx().json();
        if (jroot == null)
          return null;
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
        final String query = String.format("SELECT name, concreteType FROM entity WHERE parentId==\"%s\"", parentId);
        get(REPO_ENDPOINT, "/query?", query("query", query));
        if (resp == null || resp.getStatusLine().getStatusCode() == 403 || resp.getStatusLine().getStatusCode() == 401) {
          end();
          return null;
        }
        final JsonNode jroot = ensure2xx().json();
        if (jroot == null)
          return null;
        for (final JsonNode jchild : jroot.get("results")) {
          final String id = jchild.get("entity.id").textValue();
          final String name = jchild.get("entity.name").textValue();
          final String type = jchild.get("entity.concreteType").get(0).textValue();
          final Entity child = new Entity(id, name, type);
          children.add(child);
        }

        /*
        get(REPO_ENDPOINT, "/entity/", parentId, "/children");
        if (resp == null || resp.getStatusLine().getStatusCode() == 403 || resp.getStatusLine().getStatusCode() == 401) {
          return null;
        }
        final JsonNode jchildren = ensure2xx().json();
        if (jchildren == null)
          return null;
        for (final JsonNode jchild : jchildren.get("idList")) {
          if (super.cancelled)
            return null;
          final String id = jchild.get("id").textValue();
          get(REPO_ENDPOINT, "/entity/", id, "/bundle?mask=", Integer.toString(0x1 | 0x20));
          if (resp == null || resp.getStatusLine().getStatusCode() == 403 || resp.getStatusLine().getStatusCode() == 401) {
            end();
            continue;
          }
          final JsonNode jinfo = ensure2xx().json();
          if (jinfo == null)
            return null;
          final JsonNode jentity = jinfo.get("entity");
          final String name = jentity.get("name").textValue();
          final String type = jentity.get("entityType").textValue();
          final Entity child = new Entity(id, name, type);
          children.add(child);
        }
        */
        return children;
      }
    };
  }

  public ResultTask<String> newDescriptionMarkdownTask(final String entityId) {
    return new ReqTask<String>() {
      protected String checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get description of " + entityId);
        get(join(REPO_ENDPOINT, "/entity/", entityId, "/wiki2"));
        if (resp == null)
          return null;
        final int statusCode = resp.getStatusLine().getStatusCode();
        if (statusCode == 404) {
          end();
          return null;
        } else {
          ensure2xx();
        }
        final JsonNode jinfo = json();
        final String descriptionId = jinfo.get("id").textValue();
        final String description = get(REPO_ENDPOINT, "/entity/", entityId, "/wiki2/", descriptionId, "/markdown").ensure2xx().string();
        return description;
      }
    };
  }

  public ResultTask<List<Entity>> newSearchTask(final String queryTerm, final EntityType entityType) {
    return new ReqTask<List<Entity>>() {
      protected List<Entity> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Search for " + queryTerm);
        // Build the jquery object, which contains the query elements
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNodeFactory jsonNodeFactory = mapper.getNodeFactory();
        // Jackson doesn't support proper chaining when adding fields with JsonNodes as values :(
        final ObjectNode jquery = jsonNodeFactory.objectNode();
        jquery.set("queryTerm",    jsonNodeFactory.arrayNode().add(queryTerm));
        jquery.set("returnFields", jsonNodeFactory.arrayNode().add("name").add("id").add("node_type_r"));
        jquery.put("size",         100);
        if (entityType != null) {
          final ObjectNode jtype = jsonNodeFactory.objectNode();
          jtype.put("key",  "node_type");
          jtype.put("value", entityType.getAlias());
          jquery.set("booleanQuery", jsonNodeFactory.arrayNode().add(jtype));
        }

        final JsonNode jresults = postJson(join(REPO_ENDPOINT, "/search"), jquery).ensure2xx().json();

        final List<Entity> entities = new ArrayList<Entity>();
        for (final JsonNode jhit : jresults.get("hits")) {
          final String id = jhit.get("id").textValue();
          final String name = jhit.get("name").textValue();
          final String type = jhit.get("node_type").textValue();
          entities.add(new Entity(id, name, type));
        }
        return entities;
      }
    };
  }
}