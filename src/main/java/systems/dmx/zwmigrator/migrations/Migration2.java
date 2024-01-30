package systems.dmx.zwmigrator.migrations;

import static systems.dmx.core.Constants.*;
import static systems.dmx.workspaces.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Migration;
import systems.dmx.zwmigrator.LQ;
import systems.dmx.zwmigrator.ZW;

import java.util.List;



/**
 * Transforms a Zukunftswerk instance into a Linqa instance.
 * <p>
 * Runs ALWAYS.
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        transformWorkspaces();
        // Bilingual topics
        retypeBilingualTopics("note");
        retypeBilingualTopics("textblock");
        // Globals
        retypeTopics("language");
        retypeTopics("translation_edited");
        retypeTopics("locked");
        retypeAssocs("shared_workspace");
        // Plugin topic
        Topic plugin = dmx.getTopicByUri(ZW.ZW_PLUGIN_URI);
        plugin.update(mf.newChildTopicsModel()
            .set(PLUGIN_NAME, "DMX Linqa")
            .set(PLUGIN_SYMBOLIC_NAME, LQ.LINQA_PLUGIN_URI)
            .set(PLUGIN_MIGRATION_NR, 2)
        );
        plugin.setUri(LQ.LINQA_PLUGIN_URI);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void retypeBilingualTopics(String item) {
        dmx.getTopicsByType("zukunftswerk." + item).stream().forEach(topic -> {
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
        });
    }

    private void retypeTopics(String item) {
        dmx.getTopicsByType("zukunftswerk." + item).stream().forEach(topic -> topic.setTypeUri("linqa." + item));
    }

    private void retypeAssocs(String item) {
        dmx.getAssocsByType("zukunftswerk." + item).stream().forEach(assoc -> assoc.setTypeUri("linqa." + item));
    }

    private void transformWorkspaces() {
        // "Team" workspace: rename + change URI            // FIXME: do renaming actually?
        Topic teamWs = dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI);
        teamWs.update(
            mf.newTopicModel(LQ.LINQA_ADMIN_WS_URI, WORKSPACE, mf.newChildTopicsModel()
                .set(WORKSPACE_NAME, LQ.LINQA_ADMIN_WS_NAME)
                .set(WORKSPACE_NAME + "#" + ZW.DE, LQ.LINQA_ADMIN_WS_NAME)
            )
        );
        // Workspace names
        transformWorkspaceName(teamWs);
        getAllZWWorkspaces().stream().forEach(this::transformWorkspaceName);
        // "Workspace Name" type def
        TopicType type = dmx.getTopicType(WORKSPACE);
        String compDefUri = ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE;
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.DE).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG1));
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.FR).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG2));
    }

    private void transformWorkspaceName(Topic ws) {
        ChildTopics ct = ws.getChildTopics();
        RelatedTopic de = ct.getTopicOrNull(WORKSPACE_NAME + "#" + ZW.DE);
        RelatedTopic fr = ct.getTopicOrNull(WORKSPACE_NAME + "#" + ZW.FR);
        if (de != null) {
            de.getRelatingAssoc().setTypeUri(LQ.LANG1);
        }
        if (fr != null) {
            fr.getRelatingAssoc().setTypeUri(LQ.LANG2);
        }
    }

    private List<RelatedTopic> getAllZWWorkspaces() {
        return dmx.getTopicByUri(ZW.ZW_PLUGIN_URI).getRelatedTopics(ZW.SHARED_WORKSPACE, DEFAULT, DEFAULT, WORKSPACE);
    }
}
