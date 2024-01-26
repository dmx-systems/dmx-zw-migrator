package systems.dmx.zwmigrator.migrations;

import static systems.dmx.core.Constants.*;
import static systems.dmx.zukunftswerk.Constants.*;
import systems.dmx.core.Topic;

import systems.dmx.core.service.Migration;



/**
 * Transforms a Zukunftswerk instance into a Linqa instance.
 * <p>
 * Runs ALWAYS.
 */
public class Migration1 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // Plugin topic
        Topic pluginTopic = dmx.getTopicByUri(ZW_PLUGIN_URI);
        pluginTopic.update(mf.newChildTopicsModel()
            .set(PLUGIN_NAME, "DMX Linqa")
            .set(PLUGIN_SYMBOLIC_NAME, "systems.dmx.linqa")
            .set(PLUGIN_MIGRATION_NR, 2)
        );
        pluginTopic.setUri("systems.dmx.linqa");
    }
}
