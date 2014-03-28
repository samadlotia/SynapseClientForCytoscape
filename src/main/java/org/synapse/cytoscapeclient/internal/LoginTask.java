package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class LoginTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  @Tunable(description="User Email", gravity=1.0)
  public String userId;

  @Tunable(description="API Key", gravity=2.0)
  public String apiKey;

  public LoginTask(final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.userId = authCacheMgr.getUserID();
    this.apiKey = authCacheMgr.getAPIKey();
  }

  public void run(TaskMonitor monitor) throws Exception {
    clientMgr.set(null);
    if (userId.length() != 0 && apiKey.length() != 0) {
      monitor.setTitle("Synapse log in");

      final SynClient client = new SynClient(new APIKeyAuth(userId, apiKey));
      super.insertTasksAfterCurrentTask(client.newUserProfileTask(), new AssignClientTask(clientMgr, client));
    } else {
      // user wants to clear auth cache
      authCacheMgr.setUserIDAPIKey("", "");
    }
  }

  public void cancel() {}
}

class AssignClientTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final SynClient client;

  public AssignClientTask(final SynClientMgr clientMgr, final SynClient client) {
    this.clientMgr = clientMgr;
    this.client = client;
  }

  public void run(TaskMonitor monitor) {
    clientMgr.set(client);
  }

  public void cancenl() {}
}
