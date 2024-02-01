package systems.dmx.zwmigrator;

import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.workspaces.WorkspacesService;



public class ZWMigratorPlugin extends PluginActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private WorkspacesService ws;
}
