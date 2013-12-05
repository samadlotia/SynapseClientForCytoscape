package org.synapse.cytoscapeclient.internal;

import java.util.Properties;
import org.osgi.framework.BundleContext;
import org.cytoscape.service.util.AbstractCyActivator;

public class CyActivator extends AbstractCyActivator {
  public void start(BundleContext bc) {
  }

  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }

}
