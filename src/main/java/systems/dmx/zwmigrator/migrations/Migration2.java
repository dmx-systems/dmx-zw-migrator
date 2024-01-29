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
public class Migration1 extends Migration {

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
        for (Topic note : dmx.getTopicsByType(ZW.ZW_NOTE)) {
            note.setTypeUri(LQ.LINQA_NOTE);
            ChildTopics ct = note.getChildTopics();
            RelatedTopic de = ct.getTopic(ZW.ZW_NOTE_DE);
            RelatedTopic fr = ct.getTopicOrNull(ZW.ZW_NOTE_FR);
            RelatedTopic origLang = ct.getTopicOrNull(ZW.LANGUAGE + "#" + ZW.ORIGINAL_LANGUAGE);
            RelatedTopic edited = ct.getTopicOrNull(ZW.TRANSLATION_EDITED);
            RelatedTopic locked = ct.getTopicOrNull(ZW.LOCKED);
            de.setTypeUri(LQ.LINQA_NOTE_TEXT);
            de.getRelatingAssoc().setTypeUri(LQ.LANG1);
            if (fr != null) {
                fr.setTypeUri(LQ.LINQA_NOTE_TEXT);
                fr.getRelatingAssoc().setTypeUri(LQ.LANG2);
            }
            if (origLang != null) {
                origLang.setTypeUri(LQ.LANGUAGE);               // TODO: retype globally (not per note)
                origLang.getRelatingAssoc().setTypeUri(LQ.ORIGINAL_LANGUAGE);
            }
            if (edited != null) {
                edited.setTypeUri(LQ.TRANSLATION_EDITED);       // TODO: retype globally (not per note)
            }
            if (locked != null) {
                locked.setTypeUri(LQ.LOCKED);                   // TODO: retype globally (not per note)
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void retype(String source) {
    }
}
