package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class LoginTask implements Task {
  final AuthCacheMgr authCacheMgr;

  @Tunable(description="User Email")
  public String userId;

  @Tunable(description="Synapse API Key")
  public String apiKey;

  public LoginTask(final AuthCacheMgr authCacheMgr) {
    this.authCacheMgr = authCacheMgr;
    this.userId = authCacheMgr.getUserID();
    this.apiKey = authCacheMgr.getAPIKey();
  }

  public void run(TaskMonitor monitor) {
    if (userId.length() != 0 && apiKey.length() != 0) {
      monitor.setTitle("Synapse log in");
      monitor.setStatusMessage("Retrieving user profile");

      final SynapseClient c = SynapseClient.loginWithAPIKey(userId, apiKey);
      monitor.setStatusMessage("Welcome, " + c.getUserDisplayName() + "!");
    }
    authCacheMgr.setUserIDAPIKey(userId, apiKey);
  }

  public void cancel() {}
}