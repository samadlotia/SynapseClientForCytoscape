package org.synapse.cytoscapeclient.internal;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;

/**
 * Maintains instances of {@code InputStreamTaskFactory} that have
 * been registered through Cytoscape.
 * This class is used to find the appropriate task factory that
 * can import a given network, table, or session file.
 */
public class ImporterMgr {
  final Set<CyFileFilter> fileFilters = new HashSet<CyFileFilter>();

  public void addFactory(final InputStreamTaskFactory factory, Map<?,?> props) {
    final CyFileFilter fileFilter = factory.getFileFilter();
    synchronized(fileFilters) {
      fileFilters.add(fileFilter);
    }
  }

  public void removeFactory(final InputStreamTaskFactory factory, Map<?,?> props) {
    final CyFileFilter fileFilter = factory.getFileFilter();
    synchronized(fileFilters) {
      fileFilters.remove(fileFilter);
    }
  }

  public boolean doesImporterExist(final String extension, final DataCategory category) {
    synchronized(fileFilters) {
      for (final CyFileFilter fileFilter : fileFilters) {
        if (!fileFilter.getDataCategory().equals(category)) {
          continue;
        }
        if (fileFilter.getExtensions().contains(extension)) {
          return true;
        }
      }
    }
    return false;
  }
}
