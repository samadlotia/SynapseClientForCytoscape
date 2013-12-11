package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

class AuthCacheMgr {
  final Logger logger = Logger.getLogger(this.getClass());
  final Properties props = new Properties();
  final File authCacheFile;

  public AuthCacheMgr(final File appConfDir) {
    if (!appConfDir.isDirectory()) {
      final boolean succeeded = appConfDir.mkdirs();
      if (!succeeded) {
        logger.warn("Failed to create configuration directory: " + appConfDir);
      }
    }

    authCacheFile = new File(appConfDir, "synapse_auth_cache");

    if (authCacheFile.exists() && authCacheFile.isFile()) {
      readAuthCacheFile();
    }
  }

  public void setUserIDAPIKey(final String userId, final String apiKey) {
    props.setProperty("userId", (userId == null ? "" : userId));
    props.setProperty("apiKey", (apiKey == null ? "" : apiKey));
    writeAuthCacheFile();
  }

  public String getUserID() {
    return props.getProperty("userId", "");
  }

  public String getAPIKey() {
    return props.getProperty("apiKey", "");
  }

  private void readAuthCacheFile() {
    try {
      final Reader reader = new FileReader(authCacheFile);
      props.load(reader);
      reader.close();
    } catch (IOException e) {
      logger.warn("Unable to read: " + authCacheFile.getAbsolutePath(), e);
    }
  }

  private void writeAuthCacheFile() {
    try {
       final Writer writer = new FileWriter(authCacheFile);
       props.store(writer, "Synapse authentication cache");
       writer.flush();
       writer.close();
    } catch (IOException e) {
      logger.warn("Unable to save Synapse authentication cache", e);
    }
  }
}