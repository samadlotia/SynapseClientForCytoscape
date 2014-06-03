package org.synapse.cytoscapeclient.internal;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumSet;
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
import java.awt.Paint;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.RenderingHints;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.markdown4j.Markdown4jProcessor;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.task.read.OpenSessionTaskFactory;
import org.cytoscape.io.DataCategory;

class BrowserDialog {
  final SynClientMgr clientMgr;
  final SynClient client;
  final TaskManager taskMgr;
  final ImporterMgr importerMgr;
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final LoadTableFileTaskFactory loadTableFileTF;
  final OpenSessionTaskFactory openSeshTF;
  final JEditorPane infoPane;
  final JDialog dialog;
  final DefaultTreeModel model;
  final JTree tree;
  final JButton importBtn;
  final JButton saveBtn;
  final Markdown4jProcessor mdProcessor = new Markdown4jProcessor();
  final JLabel loadingLabel;
  final AsyncTaskMgr asyncTaskMgr;
  final JTextField searchField;
  boolean showingSearchResults = false;

  public BrowserDialog(
        final Frame parent,
        final SynClientMgr clientMgr,
        final TaskManager taskMgr,
        final ImporterMgr importerMgr,
        final LoadNetworkFileTaskFactory loadNetworkFileTF,
        final LoadTableFileTaskFactory loadTableFileTF,
        final OpenSessionTaskFactory openSeshTF) {
    this.clientMgr = clientMgr;
    client = clientMgr.get();
    this.taskMgr = taskMgr;
    this.importerMgr = importerMgr;
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.loadTableFileTF = loadTableFileTF;
    this.openSeshTF = openSeshTF;

    dialog = new JDialog(parent, "Synapse", false);

    model = new DefaultTreeModel(new DefaultMutableTreeNode());

    infoPane = new JEditorPane("text/html", "");
    infoPane.setEditable(false);

    tree = new JTree(model);

    loadingLabel = new JLabel();
    loadingLabel.setIcon(new ImageIcon(getClass().getResource("/img/loading.gif")));
    //loadingLabel.setVisible(false);

    this.asyncTaskMgr = new AsyncTaskMgr(this);

    importBtn = new JButton("Import");
    importBtn.setEnabled(false);

    saveBtn = new JButton("Save As...");
    saveBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final SynClient.Entity entity = getSelectedEntity();
        taskMgr.execute(new TaskIterator(new SaveAsTask(client, entity)));
      }
    });
    saveBtn.setEnabled(false);

    tree.addTreeSelectionListener(new UpdateDescriptionAndImportButtons());

    final EasyGBC e = new EasyGBC();
    final JLabel searchLabel = new JLabel(new ImageIcon(getClass().getResource("/img/search-icon.png")));

    final JButton cancelButton = new JButton(new ImageIcon(getClass().getResource("/img/cancel-icon.png")));
    cancelButton.setEnabled(false);
    cancelButton.setBorder(BorderFactory.createEmptyBorder());
    cancelButton.setBorderPainted(false);
    cancelButton.setContentAreaFilled(false);
    cancelButton.setFocusPainted(false);

    searchField = new JTextField();
    searchField.setOpaque(false);
    searchField.setBorder(BorderFactory.createEmptyBorder());
    searchField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) { update(); }
      public void insertUpdate(DocumentEvent e) { update(); }
      public void removeUpdate(DocumentEvent e) { update(); }

      private void update() {
        cancelButton.setEnabled(searchField.getText().length() > 0 || showingSearchResults);
      }
    });

    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        searchField.setText("");
        if (showingSearchResults) {
          showUserProjects();
        }
        searchField.requestFocus();
      }
    });

    final JPanel innerSearchPanel = new JPanel(new GridBagLayout());
    innerSearchPanel.setBorder(new SearchPanelBorder());
    innerSearchPanel.add(searchLabel, e.insets(6, 8, 6, 0));
    innerSearchPanel.add(searchField, e.right().expandH().insets(6, 8, 6, 0));
    innerSearchPanel.add(cancelButton, e.right().noExpand().insets(6, 2, 6, 8));

    final JComboBox entityTypeCombo = new JComboBox(SynClient.EntityType.values());
    entityTypeCombo.setEnabled(false);
    final JCheckBox onlyBtn = new JCheckBox("Only: ");
    onlyBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        entityTypeCombo.setEnabled(onlyBtn.isSelected());
      }
    });

    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String query = searchField.getText();
        final SynClient.EntityType type = onlyBtn.isSelected() ? (SynClient.EntityType) entityTypeCombo.getSelectedItem() : null;
        final ResultTask<List<SynClient.Entity>> searchTask = client.newSearchTask(query, type);
        asyncTaskMgr.execute(new TaskIterator(searchTask, new ShowSearchResults(query, searchTask)));
      }
    });

    final JPanel searchPanel = new JPanel(new GridBagLayout());
    searchPanel.add(innerSearchPanel, e.reset().expandH().insets(0, 0, 0, 7));
    searchPanel.add(onlyBtn, e.noInsets().right().noExpand());
    searchPanel.add(entityTypeCombo, e.right());

    final JPanel primaryPanel = new JPanel(new GridBagLayout());
    primaryPanel.add(searchPanel, e.reset().expandH().insets(3, 5, 7, 0));
    primaryPanel.add(new JScrollPane(tree), e.down().noInsets().expandHV());

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    buttonsPanel.add(importBtn);
    buttonsPanel.add(saveBtn);

    final JPanel secondaryPanel = new JPanel(new GridBagLayout());
    secondaryPanel.add(new JScrollPane(infoPane), e.reset().expandHV());
    secondaryPanel.add(buttonsPanel, e.expandH().down());

    dialog.setLayout(new GridBagLayout());
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, primaryPanel, secondaryPanel);
    splitPane.setResizeWeight(0.85);
    dialog.add(splitPane, e.reset().expandHV().noInsets());
    dialog.add(loadingLabel, e.expandH().insets(3, 4, 5, 0).down());

    dialog.pack();
    dialog.setVisible(true);

    showUserProjects();
  }

  private void showUserProjects() {
    final ResultTask<SynClient.UserProfile> userProfileTask = client.newUserProfileTask();
    asyncTaskMgr.execute(new TaskIterator(userProfileTask, new AbstractTask() {
      public void run(TaskMonitor monitor) {
        final ResultTask<List<SynClient.Entity>> projectsTask = client.newProjectsTask(userProfileTask.get());
        super.insertTasksAfterCurrentTask(projectsTask, new ShowUserProjects(userProfileTask, projectsTask));
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
    loadingLabel.setText("");
    loadingLabel.setIcon(null);
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
      updateDescription();
      updateImportBtn();
      updateSaveBtn();
    }
  }

  private void updateDescription() {
    final SynClient.Entity entity = getSelectedEntity();
    if (entity == null) {
      infoPane.setText("");
    } else {
      setEntityDescription(entity);
      final ResultTask<String> descriptionTask = client.newDescriptionMarkdownTask(entity.getId());
      asyncTaskMgr.execute(new TaskIterator(descriptionTask, new ShowDescription(entity, descriptionTask)));
    }
  }

  private EnumSet<DataCategory> getAvailableImportersForSelectedEntity() {
    final SynClient.Entity entity = getSelectedEntity();
    if (entity == null || !(SynClient.EntityType.FILE.equals(entity.getType()))) {
      // is the selected entity of a file?
      return EnumSet.noneOf(DataCategory.class);
    }

    final String extension = getExtension(entity.getName());
    if (extension == null) {
      // if there's no file name extension, return everything so the user can decide
      return EnumSet.of(DataCategory.NETWORK, DataCategory.TABLE, DataCategory.SESSION);
    }

    // go thru each relevant data category and see if an importer exists
    final EnumSet<DataCategory> importers = EnumSet.noneOf(DataCategory.class);
    for (final DataCategory category : Arrays.asList(
        DataCategory.NETWORK,
        DataCategory.TABLE,
        DataCategory.SESSION)) {
      if (importerMgr.doesImporterExist(extension, category)) {
        importers.add(category);
      }
    }
    return importers;
  }

  private void removeListeners(final JButton btn) {
    for (final ActionListener listener : btn.getActionListeners()) {
      btn.removeActionListener(listener);
    }
  }

  private void runFileReaderTaskForDataCategory(final DataCategory category, final SynClient.Entity entity) {
    Task task = null;
    switch(category) {
      case NETWORK:
      task = ImportNetworkFromSynapseTask.noTunables(loadNetworkFileTF, clientMgr, entity.getId());
      break;
      case TABLE:
      task = ImportTableFromSynapseTask.noTunables(loadTableFileTF, clientMgr, entity.getId());
      break;
      case SESSION:
      task = OpenSessionFromSynapseTask.noTunables(openSeshTF, clientMgr, entity.getId());
      break;
    }
    taskMgr.execute(new TaskIterator(task));
  }

  private void updateImportBtn() {
    removeListeners(importBtn);

    final EnumSet<DataCategory> importers = getAvailableImportersForSelectedEntity();
    if (importers.size() == 0) {
      importBtn.setText("Import");
      importBtn.setEnabled(false);
    } else if (importers.size() == 1) {
      final DataCategory category = importers.iterator().next();
      importBtn.setText("Import " + category.getDisplayName());
      importBtn.setEnabled(true);
      importBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          runFileReaderTaskForDataCategory(category, getSelectedEntity());
        }
      });
    } else {
      importBtn.setText("Import \u25be");
      importBtn.setEnabled(true);
      final JPopupMenu menu = new JPopupMenu();
      for (final DataCategory category : importers) {
        final AbstractAction action = new AbstractAction(category.getDisplayName()) {
          public void actionPerformed(ActionEvent e) {
            runFileReaderTaskForDataCategory(category, getSelectedEntity());
          }
        };
        menu.add(action);
      }
      importBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          menu.show(importBtn, 0, importBtn.getHeight());
        }
      });
    }
  }

  private void updateSaveBtn() {
    final SynClient.Entity entity = getSelectedEntity();
    final boolean canSave = entity != null && SynClient.EntityType.FILE.equals(entity.getType());
    saveBtn.setEnabled(canSave);
  }

  class ShowUserProjects extends AbstractTask {
    final ResultTask<SynClient.UserProfile> userProfileTask;
    final ResultTask<List<SynClient.Entity>> projectsTask;

    public ShowUserProjects(final ResultTask<SynClient.UserProfile> userProfileTask, final ResultTask<List<SynClient.Entity>> projectsTask) {
      this.userProfileTask = userProfileTask;
      this.projectsTask = projectsTask;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> projects = projectsTask.get();
      if (projects == null)
        return;
      final String userName = userProfileTask.get().getUserName();
      DefaultMutableTreeNode root = null;
      if (projects.size() == 0) {
        root = new DefaultMutableTreeNode(String.format("%s: no projects found", userName));
      } else {
        root = new DefaultMutableTreeNode(userName);
        for (final SynClient.Entity project : projects) {
          final DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
          root.add(projectNode);
          final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(project.getId());
          //super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, projectNode, project.getId()));
          asyncTaskMgr.execute(new TaskIterator(childrenTask, new AddChildren(childrenTask, projectNode, project.getId())));
        }
      }
      setRootLater(root);
      showingSearchResults = false;
    }

    public void cancel() {}
  }

  void setRootLater(final DefaultMutableTreeNode root) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        model.setRoot(root);
      }
    });
  }

  class AddChildren extends AbstractTask {
    final ResultTask<List<SynClient.Entity>> task;
    final DefaultMutableTreeNode parentNode;
    final String parentId;
    volatile boolean cancelled = false;

    public AddChildren(final ResultTask<List<SynClient.Entity>> task, final DefaultMutableTreeNode parentNode, final String parentId) {
      this.task = task;
      this.parentNode = parentNode;
      this.parentId = parentId;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> children = task.get();
      if (children == null)
        return;
      for (final SynClient.Entity child : children) {
        if (cancelled) {
          return;
        }

        final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            parentNode.add(childNode);
          }
        });

        if (SynClient.EntityType.FOLDER.equals(child.getType())) {
          final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(child.getId());
          //super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, childNode, child.getId()));
          asyncTaskMgr.execute(new TaskIterator(childrenTask, new AddChildren(childrenTask, childNode, child.getId())));
        }
      }
      final int level = parentNode.getLevel();
      if (level <= 3) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            tree.expandPath(new TreePath(parentNode.getPath()));
          }
        });
      }
    }

    public void cancel() {
      cancelled = true;
    }
  }

  private void setEntityDescription(final SynClient.Entity entity) {
    infoPane.setText(String.format("<html><h1>%s</h1><h3>%s</h3></html>", entity.getName(), entity.getId()));
  }

  private void setEntityDescription(final SynClient.Entity entity, final String details) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        infoPane.setText(String.format("<html><h1>%s</h1><h3>%s</h3><hr>%s</html>", entity.getName(), entity.getId(), details));
      }
    });
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
      if (descriptionMd != null) {
        final String descriptionHtml = mdProcessor.process(descriptionMd);
        setEntityDescription(entity, descriptionHtml);
      }
    }

    public void cancel() {}
  }

  class ShowSearchResults extends AbstractTask {
    final String query;
    final ResultTask<List<SynClient.Entity>> searchTask;
    public ShowSearchResults(final String query, final ResultTask<List<SynClient.Entity>> searchTask) {
      this.query = query;
      this.searchTask = searchTask;
    }

    public void run(TaskMonitor monitor) throws Exception {
      final List<SynClient.Entity> results = searchTask.get();
      DefaultMutableTreeNode root = null;
      if (results.size() == 0) {
        root = new DefaultMutableTreeNode(String.format("No results for '%s'", query));
      } else {
        root = new DefaultMutableTreeNode(String.format("Results for '%s'", query));
        for (final SynClient.Entity entity : results) {
          final SynClient.EntityType type = entity.getType();
          final DefaultMutableTreeNode node = new DefaultMutableTreeNode(entity);
          root.add(node);
          if (SynClient.EntityType.PROJECT.equals(type) ||
              SynClient.EntityType.FOLDER.equals(type)) {
            final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(entity.getId());
            //super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, node, entity.getId()));
            asyncTaskMgr.execute(new TaskIterator(childrenTask, new AddChildren(childrenTask, node, entity.getId())));
          }
        }
      }
      setRootLater(root);
      showingSearchResults = true;
    }

    public void cancel() {}
  }
}

class SearchPanelBorder extends AbstractBorder {
  final static float ARC = 25.0f;
  final static Color BORDER_COLOR = new Color(0x909090);
  final static Color BKGND_COLOR = Color.WHITE;
  final static Stroke BORDER_STROKE = new BasicStroke(1.0f);

  final RoundRectangle2D.Float borderShape = new RoundRectangle2D.Float();
  public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
    final Graphics2D g2d = (Graphics2D) g;

    final boolean aa = RenderingHints.VALUE_ANTIALIAS_ON.equals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
    final Paint oldPaint = g2d.getPaint();
    final Stroke oldStroke = g2d.getStroke();

    borderShape.setRoundRect((float) x, (float) y, (float) (w - 1), (float) (h - 1), ARC, ARC);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setColor(BKGND_COLOR);
    g2d.fill(borderShape);

    g2d.setColor(BORDER_COLOR);
    g2d.setStroke(BORDER_STROKE);
    g2d.draw(borderShape);

    g2d.setPaint(oldPaint);
    g2d.setStroke(oldStroke);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
  }
}