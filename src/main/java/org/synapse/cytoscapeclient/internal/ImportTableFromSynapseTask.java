package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

/**
 * Command task for importing a Synapse entity as a table.
 */
public class ImportTableFromSynapseTask extends AbstractTask {
  final LoadTableFileTaskFactory loadTableFileTF;
  final SynClientMgr clientMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  public ImportTableFromSynapseTask(final LoadTableFileTaskFactory loadTableFileTF, final SynClientMgr clientMgr) {
    this.loadTableFileTF = loadTableFileTF;
    this.clientMgr = clientMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    super.insertTasksAfterCurrentTask(new InternalTask(loadTableFileTF, clientMgr, entityId));
  }

  public void cancel() {}

  public static AbstractTask noTunables(final LoadTableFileTaskFactory loadTableFileTF, final SynClientMgr clientMgr, final String entityId) {
    return new InternalTask(loadTableFileTF, clientMgr, entityId);
  }
}

class InternalTask extends AbstractTask {
  final LoadTableFileTaskFactory loadTableFileTF;
  final SynClientMgr clientMgr;
  final String entityId;

  public InternalTask(final LoadTableFileTaskFactory loadTableFileTF, final SynClientMgr clientMgr, final String entityId) {
    this.loadTableFileTF = loadTableFileTF;
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
            final TaskIterator iterator = loadTableFileTF.createTaskIterator(downloadTask.get());
            super.insertTasksAfterCurrentTask(iterator);
          }
        });
      }
    });
  }
}
