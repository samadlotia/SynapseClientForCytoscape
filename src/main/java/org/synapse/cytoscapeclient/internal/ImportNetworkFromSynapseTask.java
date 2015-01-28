package org.synapse.cytoscapeclient.internal;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;


/**
 * Command task factory for importing a Synapse entity as a network.
 */
public class ImportNetworkFromSynapseTask extends AbstractTask {
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final SynClientMgr clientMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  public ImportNetworkFromSynapseTask(final LoadNetworkFileTaskFactory loadNetworkFileTF, final SynClientMgr clientMgr) {
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.clientMgr = clientMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    super.insertTasksAfterCurrentTask(new InternalImport(loadNetworkFileTF, clientMgr, entityId));
  }

  public void cancel() {}

  public static AbstractTask noTunables(final LoadNetworkFileTaskFactory loadNetworkFileTF, final SynClientMgr clientMgr, final String entityId) {
    return new InternalImport(loadNetworkFileTF, clientMgr, entityId);
  }
}

class InternalImport extends AbstractTask {
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final SynClientMgr clientMgr;
  final String entityId;

  public InternalImport(final LoadNetworkFileTaskFactory loadNetworkFileTF, final SynClientMgr clientMgr, final String entityId) {
    this.loadNetworkFileTF = loadNetworkFileTF;
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

    monitor.setTitle("Import network from Synapse");
    final ResultTask<SynClient.SynFile> fileInfoTask = client.newFileInfoTask(entityId);
    super.insertTasksAfterCurrentTask(fileInfoTask, new AbstractTask() {
      public void run(TaskMonitor monitor) {
        final ResultTask<File> downloadTask = client.newDownloadFileTask(fileInfoTask.get());
        super.insertTasksAfterCurrentTask(downloadTask, new AbstractTask() {
          public void run(TaskMonitor monitor) {
            final TaskIterator iterator = loadNetworkFileTF.createTaskIterator(downloadTask.get(), new SetupNetwork(fileInfoTask.get()));
            super.insertTasksAfterCurrentTask(iterator);
          }
        });
      }
    });
  }
}

class SetupNetwork implements TaskObserver {
  final SynClient.SynFile fileInfo;

  public SetupNetwork(final SynClient.SynFile fileInfo) {
    this.fileInfo = fileInfo;
  }

  public void taskFinished(final ObservableTask task) {
    final List<CyNetworkView> views = (List<CyNetworkView>) task.getResults(List.class);
    if (views.size() != 1) {
      return;
    }
    final String netName = fileInfo.getFilename();
    final CyNetwork net = views.get(0).getModel();
    net.getRow(net).set(CyNetwork.NAME, netName);
    if (net instanceof CySubNetwork) {
      final CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
      root.getRow(root).set(CyNetwork.NAME, netName);
    }
  }

  public void allFinished(final FinishStatus finishStatus) {}
}
