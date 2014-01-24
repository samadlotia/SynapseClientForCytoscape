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
  final CyTableReaderManager tableReaderMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile InputStream fileContents = null;
  volatile boolean cancelled = false;

  public ImportTableFromSynapseTask(final CyTableReaderManager tableReaderMgr) {
    this.tableReaderMgr = tableReaderMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (entityId == null || entityId.length() == 0)
      return;

    monitor.setTitle("Import table from Synapse");
    monitor.setStatusMessage("Getting entity information");
    final SynapseClient.SynFile file = SynapseClient.get().getFile(entityId);

    monitor.setStatusMessage("Reading Synapse file: " + file.name);
    final CyTableReader tableReader = tableReaderMgr.getReader(file.file.toURI(), file.name);
    if (tableReader == null)
      throw new Exception("Unsupported table file type: " + file.name);

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