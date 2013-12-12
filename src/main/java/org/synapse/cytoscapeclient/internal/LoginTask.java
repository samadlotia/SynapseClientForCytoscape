package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class LoginTask implements Task {
  final AuthCacheMgr authCacheMgr;

  @Tunable(description="User Email", gravity=1.0)
  public String userId;

  @Tunable(description="API Key", gravity=2.0)
  public String apiKey;

  public LoginTask(final AuthCacheMgr authCacheMgr) {
    this.authCacheMgr = authCacheMgr;
    this.userId = authCacheMgr.getUserID();
    this.apiKey = authCacheMgr.getAPIKey();
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (userId.length() != 0 && apiKey.length() != 0) {
      monitor.setTitle("Synapse log in");
      monitor.setStatusMessage("Retrieving user profile");

      SynapseClient.loginWithAPIKey(userId, apiKey);
    }
    authCacheMgr.setUserIDAPIKey(userId, apiKey);
  }

  public void cancel() {}
}