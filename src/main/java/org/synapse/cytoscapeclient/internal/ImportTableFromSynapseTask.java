package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

public class ImportTableFromSynapseTask extends AbstractTask {
  final LoadTableFileTaskFactory loadTableFileTF;
  final SynClientMgr clientMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile MaybeTask<SynClient.SynFile> task = null;
  volatile boolean cancelled = false;

  public ImportTableFromSynapseTask(final LoadTableFileTaskFactory loadTableFileTF, final SynClientMgr clientMgr) {
    this.loadTableFileTF = loadTableFileTF;
    this.clientMgr = clientMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (entityId == null || entityId.length() == 0)
      return;

    final SynClient client = clientMgr.get();
    if (client == null) { // not logged in, so just exit
      return;
    }

    monitor.setTitle("Import table from Synapse");
    monitor.setStatusMessage("Getting entity information");
    task = client.newGetFileTask(entityId);
    final SynClient.SynFile file = task.run(monitor).get();
    task = null;
    if (file == null) { // user cancelled, so exit
      return;
    }

    super.insertTasksAfterCurrentTask(loadTableFileTF.createTaskIterator(file.getFile()));
  }

  public void cancel() {
    cancelled = true;
    if (task != null) {
      task.cancel();
    }
  }
}