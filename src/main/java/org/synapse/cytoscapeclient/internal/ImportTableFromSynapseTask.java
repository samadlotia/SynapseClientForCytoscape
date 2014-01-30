package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import org.cytoscape.io.read.CyTableReader;
import org.cytoscape.io.read.CyTableReaderManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class ImportTableFromSynapseTask extends AbstractTask {
  final SynClientMgr clientMgr;
  final CyTableReaderManager tableReaderMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile InputStream fileContents = null;
  volatile boolean cancelled = false;

  public ImportTableFromSynapseTask(final SynClientMgr clientMgr, final CyTableReaderManager tableReaderMgr) {
    this.clientMgr = clientMgr;
    this.tableReaderMgr = tableReaderMgr;
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
    final SynClient.SynFile file = client.newGetFileTask(entityId).run(monitor).get();
    if (file == null) { // user cancelled, so exit
      return;
    }

    monitor.setStatusMessage("Reading Synapse file: " + file.getName());
    final CyTableReader tableReader = tableReaderMgr.getReader(file.getFile().toURI(), file.getName());
    if (tableReader == null)
      throw new Exception("Unsupported table file type: " + file.getName());

    super.insertTasksAfterCurrentTask(tableReader, new AbstractTask() {
      volatile boolean cancelled = false;
      public void run(TaskMonitor monitor) throws Exception {
      }

      public void cancel() {
        cancelled = true;
      }
    });
  }

  public void cancel() {
    cancelled = true;
    if (fileContents != null) {
      try {
        fileContents.close();
      } catch (IOException e) {}
    }
  }
}