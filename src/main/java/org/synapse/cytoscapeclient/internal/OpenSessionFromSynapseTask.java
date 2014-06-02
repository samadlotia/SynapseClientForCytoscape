package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.task.read.OpenSessionTaskFactory;

public class OpenSessionFromSynapseTask extends AbstractTask {
  final OpenSessionTaskFactory openSeshTF;
  final SynClientMgr clientMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  public OpenSessionFromSynapseTask(final OpenSessionTaskFactory openSeshTF, final SynClientMgr clientMgr) {
    this.openSeshTF = openSeshTF;
    this.clientMgr = clientMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    super.insertTasksAfterCurrentTask(new InternalTask(openSeshTF, clientMgr, entityId));
  }

  public void cancel() {}

  public static AbstractTask noTunables(final OpenSessionTaskFactory openSeshTF, final SynClientMgr clientMgr, final String entityId) {
    return new InternalTask(openSeshTF, clientMgr, entityId);
  }

  static class InternalTask extends AbstractTask {
    final OpenSessionTaskFactory openSeshTF;
    final SynClientMgr clientMgr;
    final String entityId;

    public InternalTask(final OpenSessionTaskFactory openSeshTF, final SynClientMgr clientMgr, final String entityId) {
      this.openSeshTF = openSeshTF;
      this.clientMgr = clientMgr;
      this.entityId = entityId;
    }

    public void run(TaskMonitor monitor) throws Exception {
      if (entityId == null || entityId.length() == 0)
        return;

      final SynClient client = clientMgr.get();
      if (client == null) { // not logged in, so just exit
        return;
      }

      monitor.setTitle("Import table from Synapse");
      final ResultTask<SynClient.SynFile> fileInfoTask = client.newFileInfoTask(entityId);
      super.insertTasksAfterCurrentTask(fileInfoTask, new AbstractTask() {
        public void run(TaskMonitor monitor) {
          final ResultTask<File> downloadTask = client.newDownloadFileTask(fileInfoTask.get());
          super.insertTasksAfterCurrentTask(downloadTask, new AbstractTask() {
            public void run(TaskMonitor monitor) {
              final TaskIterator iterator = openSeshTF.createTaskIterator(downloadTask.get());
              super.insertTasksAfterCurrentTask(iterator);
            }
          });
        }
      });
    }
  }
}
