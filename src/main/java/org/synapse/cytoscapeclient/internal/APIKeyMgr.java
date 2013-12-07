package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

class APIKeyMgr {
  final File conf;
  final Logger logger = Logger.getLogger(this.getClass());
  String apiKey = "";

  public APIKeyMgr(final File appConfDir) {
    if (!appConfDir.isDirectory()) {
      final boolean succeeded = appConfDir.mkdirs();
      if (!succeeded) {
        logger.warn("Failed to create configuration directory: " + appConfDir);
      }
    }

    conf = new File(appConfDir, "synapse_api_key");

    if (conf.exists() && conf.isFile()) {
      try {
        final BufferedReader reader = new BufferedReader(new FileReader(conf));
        apiKey = reader.readLine();
        reader.close();
      } catch (IOException e) {
        logger.warn("Unable to read Synapse API key", e);
      }
    }
  }

  public void set(String apiKey) {
    if (apiKey == null)
      apiKey = "";
    this.apiKey = apiKey;

    System.out.println("Set: \"" + apiKey + "\"");

    try {
       final FileWriter writer = new FileWriter(conf);
       writer.write(apiKey);
       writer.flush();
       writer.close();
    } catch (IOException e) {
      logger.warn("Unable to save Synapse API key", e);
    }
  }

  public String get() {
    return apiKey;
  }
}