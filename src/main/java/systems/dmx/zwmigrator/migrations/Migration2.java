package systems.dmx.zwmigrator.migrations;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;
import systems.dmx.zwmigrator.LQ;
import systems.dmx.zwmigrator.ZW;



/**
 * Transforms a Zukunftswerk instance into a Linqa instance.
 * <p>
 * Runs ALWAYS.
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        //
        // Plugin topic
        Topic plugin = dmx.getTopicByUri(ZW.ZW_PLUGIN_URI);
        plugin.update(mf.newChildTopicsModel()
            .set(PLUGIN_NAME, "DMX Linqa")
            .set(PLUGIN_SYMBOLIC_NAME, LQ.LINQA_PLUGIN_URI)
            .set(PLUGIN_MIGRATION_NR, 2)
        );
        plugin.setUri(LQ.LINQA_PLUGIN_URI);
        //
        // Note topics
        retypeBilingualTopics("note");
        // Globals
        retypeTopics("language");
        retypeTopics("translation_edited");
        retypeTopics("locked");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void retypeBilingualTopics(String item) {
        for (Topic topic : dmx.getTopicsByType("zukunftswerk." + item)) {
            ChildTopics ct = topic.getChildTopics();
            // text
            RelatedTopic de = ct.getTopic("zukunftswerk." + item + ".de");
            RelatedTopic fr = ct.getTopicOrNull("zukunftswerk." + item + ".fr");
            de.setTypeUri("linqa." + item + "_text");
            de.getRelatingAssoc().setTypeUri(LQ.LANG1);
            if (fr != null) {
                fr.setTypeUri("linqa." + item + "_text");
                fr.getRelatingAssoc().setTypeUri(LQ.LANG2);
            }
            // language
            RelatedTopic origLang = ct.getTopicOrNull(ZW.LANGUAGE + "#" + ZW.ORIGINAL_LANGUAGE);
            if (origLang != null) {
                origLang.getRelatingAssoc().setTypeUri(LQ.ORIGINAL_LANGUAGE);
            }
            // retype composite
            topic.setTypeUri("linqa." + item);
        }
    }

    private void retypeTopics(String item) {
        for (Topic topic : dmx.getTopicsByType("zukunftswerk." + item)) {
            topic.setTypeUri("linqa." + item);
        }
    }
}
