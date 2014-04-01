package org.synapse.cytoscapeclient.internal;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.io.read.InputStreamTaskFactory;

public class CyActivator extends AbstractCyActivator {
  public void start(BundleContext bc) {
    System.out.println((new java.util.Date()).toString() + " started: " + getClass().getName());
    
    final LoadNetworkFileTaskFactory loadNetworkFileTF = getService(bc, LoadNetworkFileTaskFactory.class);
    final LoadTableFileTaskFactory loadTableFileTF = getService(bc, LoadTableFileTaskFactory.class);
    final CyApplicationConfiguration cyAppConf = getService(bc, CyApplicationConfiguration.class);
    final CySwingApplication cySwingApp = getService(bc, CySwingApplication.class);
    final TaskManager taskMgr = getService(bc, DialogTaskManager.class);

    final SynClientMgr clientMgr = new SynClientMgr();
    final AuthCacheMgr authCacheMgr = new AuthCacheMgr(cyAppConf.getAppConfigurationDirectoryLocation(this.getClass()));

    final ImporterMgr importerMgr = new ImporterMgr();
    registerServiceListener(bc, importerMgr, "addFactory", "removeFactory", InputStreamTaskFactory.class);

    registerService(bc, new LoginTaskFactory(clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Login...",
      ServiceProperties.PREFERRED_MENU, "Apps.Synapse"
    ));

    registerService(bc, new BrowseTaskFactory(cySwingApp, clientMgr, taskMgr, authCacheMgr, importerMgr, loadNetworkFileTF, loadTableFileTF), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Browse...",
      ServiceProperties.PREFERRED_MENU, "Apps.Synapse"
    ));

    registerService(bc, new ImportNetworkFromSynapseTaskFactory(loadNetworkFileTF, clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "From Synapse...",
      ServiceProperties.PREFERRED_MENU, "File.Import.Network"
    ));

    registerService(bc, new ImportTableFromSynapseTaskFactory(loadTableFileTF, clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "From Synapse...",
      ServiceProperties.PREFERRED_MENU, "File.Import.Table"
    ));
  }

  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }
}
