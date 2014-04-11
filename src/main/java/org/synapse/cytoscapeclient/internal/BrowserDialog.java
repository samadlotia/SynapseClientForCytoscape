package org.synapse.cytoscapeclient.internal;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.RenderingHints;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import javax.swing.border.AbstractBorder;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.markdown4j.Markdown4jProcessor;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.io.DataCategory;

class BrowserDialog {
  final SynClient client;
  final TaskManager taskMgr;
  final ImporterMgr importerMgr;
  final JEditorPane infoPane;
  final JDialog dialog;
  final DefaultTreeModel model;
  final JTree tree;
  final JButton importNetworkBtn;
  final JButton importTableBtn;
  final Markdown4jProcessor mdProcessor = new Markdown4jProcessor();
  final JLabel loadingLabel;
  final AsyncTaskMgr asyncTaskMgr;
  final JTextField searchField;

  public BrowserDialog(
        final Frame parent,
        final SynClientMgr clientMgr,
        final TaskManager taskMgr,
        final ImporterMgr importerMgr,
        final LoadNetworkFileTaskFactory loadNetworkFileTF,
        final LoadTableFileTaskFactory loadTableFileTF) {
    client = clientMgr.get();
    this.taskMgr = taskMgr;
    this.importerMgr = importerMgr;
    dialog = new JDialog(parent, "Browse Synapse", false);
    model = new DefaultTreeModel(new DefaultMutableTreeNode());
    infoPane = new JEditorPane("text/html", "");
    infoPane.setEditable(false);
    tree = new JTree(model);
    loadingLabel = new JLabel();
    loadingLabel.setIcon(new ImageIcon(getClass().getResource("/img/loading.gif")));
    loadingLabel.setVisible(false);
    this.asyncTaskMgr = new AsyncTaskMgr(this);
    searchField = new JTextField();
    searchField.setOpaque(false);
    searchField.setBorder(BorderFactory.createEmptyBorder());

    importNetworkBtn = new JButton("Import as Network");
    importNetworkBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final SynClient.Entity entity = getSelectedEntity();
        taskMgr.execute(new TaskIterator(ImportNetworkFromSynapseTask.noTunables(loadNetworkFileTF, clientMgr, entity.getId())));
      }
    });
    importNetworkBtn.setEnabled(false);

    importTableBtn = new JButton("Import as Table");
    importTableBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final SynClient.Entity entity = getSelectedEntity();
        taskMgr.execute(new TaskIterator(ImportTableFromSynapseTask.noTunables(loadTableFileTF, clientMgr, entity.getId())));
      }
    });
    importTableBtn.setEnabled(false);

    tree.addTreeSelectionListener(new UpdateDescriptionAndImportButtons());

    final EasyGBC e = new EasyGBC();
    final JLabel searchLabel = new JLabel(new ImageIcon(getClass().getResource("/img/search-icon.png")));
    final JPanel searchPanel = new JPanel(new GridBagLayout());

    final JButton cancelButton = new JButton(new ImageIcon(getClass().getResource("/img/cancel-icon.png")));
    cancelButton.setEnabled(false);
    cancelButton.setBorder(BorderFactory.createEmptyBorder());
    cancelButton.setBorderPainted(false);
    cancelButton.setContentAreaFilled(false);
    cancelButton.setFocusPainted(false);

    searchPanel.setBorder(new SearchPanelBorder());
    searchPanel.add(searchLabel, e.insets(4, 6, 4, 0));
    searchPanel.add(searchField, e.right().expandH().insets(4, 6, 4, 0));
    searchPanel.add(cancelButton, e.right().noExpand().insets(4, 2, 4, 6));

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    buttonsPanel.add(importNetworkBtn);
    buttonsPanel.add(importTableBtn);

    final JPanel secondaryPanel = new JPanel(new GridBagLayout());
    secondaryPanel.add(new JScrollPane(infoPane), e.reset().expandHV());
    secondaryPanel.add(buttonsPanel, e.expandH().down());

    dialog.setLayout(new GridBagLayout());
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), secondaryPanel);
    splitPane.setResizeWeight(0.85);
    dialog.add(searchPanel, e.reset().expandH().insets(3, 3, 1, 3));
    dialog.add(splitPane, e.down().expandHV().noInsets());
    dialog.add(loadingLabel, e.expandH().insets(3, 4, 5, 0).down());

    dialog.pack();
    dialog.setVisible(true);

    final ResultTask<SynClient.UserProfile> userProfileTask = client.newUserProfileTask();
    asyncTaskMgr.execute(new TaskIterator(userProfileTask, new AbstractTask() {
      public void run(TaskMonitor monitor) {
        final ResultTask<List<SynClient.Entity>> projectsTask = client.newProjectsTask(userProfileTask.get());
        super.insertTasksAfterCurrentTask(projectsTask, new AddProjects(userProfileTask, projectsTask));
      }

      public void cancel() {}
    }));
  }

  public void setLoadingText(final String text) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (!loadingLabel.isVisible())
          loadingLabel.setVisible(true);
        loadingLabel.setText(text);
      }
    });
  }

  public void loadingDone() {
    loadingLabel.setVisible(false);
  }

  private SynClient.Entity getSelectedEntity() {
    final TreePath path = tree.getSelectionPath();
    if (path == null) {
      return null;
    }
    final Object selection = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    if (!(selection instanceof SynClient.Entity)) {
      return null;
    }
    return (SynClient.Entity) selection;
  }

  private static String getExtension(final String name) {
    final String[] pieces = name.split("\\.");
    if (pieces == null)
      return null;
    return pieces[pieces.length - 1];
  }

  class UpdateDescriptionAndImportButtons implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      final SynClient.Entity entity = getSelectedEntity();
      if (entity == null) {
        infoPane.setText("");
      } else {
        setEntityDescription(entity);
        final ResultTask<String> descriptionIdTask = client.newDescriptionIdTask(entity.getId());
        asyncTaskMgr.execute(new TaskIterator(descriptionIdTask, new GetDescription(entity, descriptionIdTask)));
      }

      boolean enableNetworkBtn = false;
      boolean enableTableBtn = false;
      if (entity != null && entity.getType().endsWith("FileEntity")) {
        final String extension = getExtension(entity.getName());
        if (extension == null) {
          enableNetworkBtn = enableTableBtn = true;
        } else {
          enableNetworkBtn = importerMgr.doesImporterExist(extension, DataCategory.NETWORK);
          enableTableBtn = importerMgr.doesImporterExist(extension, DataCategory.TABLE);
        }
      }
      importNetworkBtn.setEnabled(enableNetworkBtn);
      importTableBtn.setEnabled(enableTableBtn);
    }
  }

  class AddProjects extends AbstractTask {
    final ResultTask<SynClient.UserProfile> userProfileTask;
    final ResultTask<List<SynClient.Entity>> projectsTask;

    public AddProjects(final ResultTask<SynClient.UserProfile> userProfileTask, final ResultTask<List<SynClient.Entity>> projectsTask) {
      this.userProfileTask = userProfileTask;
      this.projectsTask = projectsTask;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> projects = projectsTask.get();
      if (projects == null)
        return;
      final DefaultMutableTreeNode root = new DefaultMutableTreeNode(userProfileTask.get().getUserName());
      for (final SynClient.Entity project : projects) {
        final DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
        root.add(projectNode);
        final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(project.getId());
        super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, projectNode, project.getId()));
      }
      model.setRoot(root);
    }

    public void cancel() {}
  }

  static boolean hasChildren(final SynClient.Entity entity) {
    final String type = entity.getType();
    return type.endsWith("Folder");
  }

  class AddChildren extends AbstractTask {
    final ResultTask<List<SynClient.Entity>> task;
    final DefaultMutableTreeNode parentNode;
    final String parentId;

    public AddChildren(final ResultTask<List<SynClient.Entity>> task, final DefaultMutableTreeNode parentNode, final String parentId) {
      this.task = task;
      this.parentNode = parentNode;
      this.parentId = parentId;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> children = task.get();
      for (final SynClient.Entity child : children) {
        final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        parentNode.add(childNode);

        if (hasChildren(child)) {
          final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(child.getId());
          super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, childNode, child.getId()));
        }
      }
      tree.expandPath(new TreePath(parentNode.getPath()));
    }

    public void cancel() {}
  }

  private void setEntityDescription(final SynClient.Entity entity) {
    infoPane.setText(String.format("<html><h1>%s</h1><h3>%s</h3></html>", entity.getName(), entity.getId()));
  }

  private void setEntityDescription(final SynClient.Entity entity, final String details) {
    infoPane.setText(String.format("<html><h1>%s</h1><h3>%s</h3><br>%s</html>", entity.getName(), entity.getId(), details));
  }

  class GetDescription extends AbstractTask {
    final SynClient.Entity entity;
    final ResultTask<String> descriptionIdTask;

    public GetDescription(final SynClient.Entity entity, final ResultTask<String> descriptionIdTask) {
      this.entity = entity;
      this.descriptionIdTask = descriptionIdTask;
    }

    public void run(TaskMonitor monitor) {
      if (getSelectedEntity() != entity)
        return;
      final String descriptionId = descriptionIdTask.get();
      if (descriptionId == null)
        return;
      final ResultTask<String> descriptionTask = client.newDescriptionMarkdownTask(entity.getId(), descriptionId);
      super.insertTasksAfterCurrentTask(descriptionTask, new ShowDescription(entity, descriptionTask));
    }

    public void cancel() {}
  }

  class ShowDescription extends AbstractTask {
    final SynClient.Entity entity;
    final ResultTask<String> descriptionTask;

    public ShowDescription(final SynClient.Entity entity, final ResultTask<String> descriptionTask) {
      this.entity = entity;
      this.descriptionTask = descriptionTask;
    }

    public void run(TaskMonitor monitor) throws Exception {
      if (getSelectedEntity() != entity)
        return;
      final String descriptionMd = descriptionTask.get();
      final String descriptionHtml = mdProcessor.process(descriptionMd);
      setEntityDescription(entity, descriptionHtml);
    }

    public void cancel() {}
  }
}

class SearchPanelBorder extends AbstractBorder {
  final static float ARC = 6.0f;
  final static Color BORDER_COLOR = new Color(0x606060);
  final static Color BKGND_COLOR = Color.WHITE;
  final static Stroke BORDER_STROKE = new BasicStroke(1.0f);

  final RoundRectangle2D.Float borderShape = new RoundRectangle2D.Float();
  public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
    final Graphics2D g2d = (Graphics2D) g;

    final boolean aa = RenderingHints.VALUE_ANTIALIAS_ON.equals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
    final Color oldColor = g2d.getColor();
    final Stroke oldStroke = g2d.getStroke();

    borderShape.setRoundRect((float) x, (float) y, (float) (w - 1), (float) (h - 1), ARC, ARC);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setColor(BKGND_COLOR);
    g2d.fill(borderShape);

    g2d.setColor(BORDER_COLOR);
    g2d.setStroke(BORDER_STROKE);
    g2d.draw(borderShape);

    g2d.setColor(oldColor);
    g2d.setStroke(oldStroke);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
  }
}