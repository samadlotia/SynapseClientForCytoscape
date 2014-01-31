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
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;


public class ImportNetworkFromSynapseTask extends AbstractTask {
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final SynClientMgr clientMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile MaybeTask<SynClient.SynFile> task = null;

  public ImportNetworkFromSynapseTask(final LoadNetworkFileTaskFactory loadNetworkFileTF, final SynClientMgr clientMgr) {
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.clientMgr = clientMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (entityId == null || entityId.length() == 0)
      return;

    final SynClient client = clientMgr.get();
    if (client == null) { // not logged in, so just exit
      return;
    }

    monitor.setTitle("Import network from Synapse");
    task = client.newGetFileTask(entityId);
    final SynClient.SynFile file = task.run(monitor).get();
    task = null;
    if (file == null) { // user cancelled, so exit
      return;
    }

    super.insertTasksAfterCurrentTask(loadNetworkFileTF.createTaskIterator(file.getFile(), new TaskObserver() {
      public void allFinished(final FinishStatus finishStatus) {}

      public void taskFinished(final ObservableTask task) {
        final List<CyNetworkView> views = (List<CyNetworkView>) task.getResults(List.class);
        if (views.size() != 1) {
          return;
        }
        final CyNetwork net = views.get(0).getModel();
        net.getRow(net).set(CyNetwork.NAME, file.getName());
        if (net instanceof CySubNetwork) {
          final CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
          root.getRow(root).set(CyNetwork.NAME, file.getName());
        }
      }
    }));
  }

  public void cancel() {
    cancelled = true;
    if (task != null) {
      task.cancel();
    }
  }
}
