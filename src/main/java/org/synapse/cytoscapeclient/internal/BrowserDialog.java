package org.synapse.cytoscapeclient.internal;

import java.util.List;

import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class BrowserDialog {
  final SynClient client;
  final TaskManager taskMgr;
  final JDialog dialog;
  final DefaultMutableTreeNode root;
  final JTree tree;

  public BrowserDialog(final Frame parent, final SynClientMgr clientMgr, final TaskManager taskMgr) {
    client = clientMgr.get();
    this.taskMgr = taskMgr;
    dialog = new JDialog(parent, "Browse Synapse", false);
    root = new DefaultMutableTreeNode(client.getUserName());
    tree = new JTree(root);

    dialog.setLayout(new GridLayout(0, 1));
    dialog.add(new JScrollPane(tree));

    dialog.pack();
    dialog.setVisible(true);

    taskMgr.execute(new TaskIterator(new AddProjects()));
  }

  class AddProjects extends AbstractTask {
    MaybeTask<List<SynClient.Project>> maybeTask = null;
    public void run(final TaskMonitor monitor) throws Exception {
      maybeTask = client.newProjectsTask();
      final List<SynClient.Project> projects = maybeTask.run(monitor).get();
      maybeTask = null;
      if (projects == null)
        return;
      root.removeAllChildren();
      for (final SynClient.Project project : projects) {
        final DefaultMutableTreeNode child = new DefaultMutableTreeNode(project.getName());
        root.add(child);
      }
    }

    public void cancel() {
      final MaybeTask<List<SynClient.Project>> maybeTask2 = maybeTask;
      if (maybeTask2 != null) {
        maybeTask2.cancel();
      }
    }
  }
}