package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class LoginTask implements Task {
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

  MaybeTask<String> maybeTask = null;

  public void run(TaskMonitor monitor) throws Exception {
    if (userId.length() != 0 && apiKey.length() != 0) {
      monitor.setTitle("Synapse log in");

      final SynClient client = new SynClient(new APIKeyAuth(userId, apiKey));
      maybeTask = client.newGetOwnerTask();
      final Maybe<String> maybe = maybeTask.run(monitor);
      maybeTask = null;

      if (maybe.get() == null) {
        clientMgr.set(null);
        return;
      } else {
        clientMgr.set(client);
      }
    }
    authCacheMgr.setUserIDAPIKey(userId, apiKey);
  }

  public void cancel() {
    if (maybeTask != null) {
      maybeTask.cancel();
    }
  }
}