package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

/**
 * Command task for logging into Synapse.
 */
public class LoginTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  @Tunable(description="Username", gravity=1.0, tooltip="Enter your username here, located in the top-right corner of the Synapse website in parenthesis")
  public String userId;

  @Tunable(description="API Key", gravity=2.0, tooltip="The API Key can be found by clicking on the \"Settings\" icon on the top-right corner of the Synapse website")
  public String apiKey;

  public LoginTask(final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.userId = authCacheMgr.getUserID();
    this.apiKey = authCacheMgr.getAPIKey();
  }

  public void run(TaskMonitor monitor) {
    super.insertTasksAfterCurrentTask(new InternalLoginTask(
      clientMgr,
      authCacheMgr,
      userId,
      apiKey
      ));
  }

  public void cancel() {}

  public static AbstractTask noTunables(
      final SynClientMgr clientMgr,
      final AuthCacheMgr authCacheMgr,
      final String userId,
      final String apiKey
    ) {
    return new InternalLoginTask(
      clientMgr,
      authCacheMgr,
      userId,
      apiKey
      );
  }
}

class InternalLoginTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;
  final String userId;
  final String apiKey;

  public InternalLoginTask(
      final SynClientMgr clientMgr,
      final AuthCacheMgr authCacheMgr,
      final String userId,
      final String apiKey
    ) {
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.userId = userId;
    this.apiKey = apiKey;
  }

  public void run(TaskMonitor monitor) throws Exception {
    clientMgr.set(null);
    if (userId.length() != 0 && apiKey.length() != 0) {
      monitor.setTitle("Synapse log in");

      final SynClient client = new SynClient(new APIKeyAuth(userId, apiKey));
      super.insertTasksAfterCurrentTask(client.newUserProfileTask(), new AssignClientTask(clientMgr, authCacheMgr, client, userId, apiKey));
    } else {
      // user wants to clear auth cache
      authCacheMgr.setUserIDAPIKey("", "");
    }
  }
}

class AssignClientTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;
  final SynClient client;
  final String userId;
  final String apiKey;

  public AssignClientTask(final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr, final SynClient client, final String userId, final String apiKey) {
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.client = client;
    this.userId = userId;
    this.apiKey = apiKey;
  }

  public void run(TaskMonitor monitor) {
    clientMgr.set(client);
    authCacheMgr.setUserIDAPIKey(userId, apiKey);
  }

  public void cancenl() {}
}
