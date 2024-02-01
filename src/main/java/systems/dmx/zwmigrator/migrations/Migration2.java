package systems.dmx.zwmigrator.migrations;

import static systems.dmx.core.Constants.*;
import static systems.dmx.topicmaps.Constants.*;
import static systems.dmx.workspaces.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;
import systems.dmx.zwmigrator.LQ;
import systems.dmx.zwmigrator.ZW;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;



/**
 * Transforms a Zukunftswerk instance into a Linqa instance.
 * <p>
 * Runs ALWAYS.
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private WorkspacesService wss;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        transformProperties();
        //
        retypeBilingualTopics("note");
        retypeBilingualTopics("textblock");
        //
        retypeTopics("language");
        retypeTopics("translation_edited");
        retypeTopics("locked");
        retypeAssocs("shared_workspace");
        //
        transformWorkspaces();
        transformPluginTopic();
        //
        // throw new RuntimeException("SUCCESS!");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void transformProperties() {
        forAllWorkspaces(ws -> {
            String wsName = ws.getSimpleValue().toString();
            logger.info("---------------- \"" + wsName + "\" ----------------");
            List<Topic> topicmaps = wss.getAssignedTopics(ws.getId(), TOPICMAP);
            if (topicmaps.size() != 1) {
                throw new RuntimeException("Workspace " + ws.getId() + " has " + topicmaps.size() +
                    " topicmaps, expected is 1");
            }
            Topic topicmap = topicmaps.get(0);                                     // othersTopicTypeUri=null
            topicmap.getRelatedTopics(TOPICMAP_CONTEXT, DEFAULT, TOPICMAP_CONTENT, null).stream().forEach(topic -> {
                Assoc assoc = topic.getRelatingAssoc();
                if (assoc.hasProperty(ZW.ZW_COLOR)) {      // Color is an optional view prop
                    assoc.setProperty(LQ.LINQA_COLOR, assoc.getProperty(ZW.ZW_COLOR), false);   // addToIndex=false
                    assoc.removeProperty(ZW.ZW_COLOR);
                }
                if (assoc.hasProperty(ZW.ANGLE)) {         // Angle is an optional view prop
                    assoc.setProperty(LQ.ANGLE, assoc.getProperty(ZW.ANGLE), false);            // addToIndex=false
                    assoc.removeProperty(ZW.ANGLE);
                }
            });
        });
    }

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
        // Workspace names
        forAllWorkspaces(this::transformWorkspaceName);
        // "Workspace Name" type def
        TopicType type = dmx.getTopicType(WORKSPACE);
        String compDefUri = ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE;
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.DE).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG1));
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.FR).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG2));
        // "Team" workspace: rename + change URI            // FIXME: do renaming actually?
        dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI).update(
            mf.newTopicModel(LQ.LINQA_ADMIN_WS_URI, WORKSPACE, mf.newChildTopicsModel()
                .set(WORKSPACE_NAME, LQ.LINQA_ADMIN_WS_NAME)
                .set(WORKSPACE_NAME + "#" + ZW.DE, LQ.LINQA_ADMIN_WS_NAME)
            )
        );
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

    private void forAllWorkspaces(Consumer<Topic> consumer) {
        consumer.accept(dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI));
        getAllZWWorkspaces().stream().forEach(consumer);
    }

    private List<RelatedTopic> getAllZWWorkspaces() {
        return dmx.getTopicByUri(ZW.ZW_PLUGIN_URI).getRelatedTopics(ZW.SHARED_WORKSPACE, DEFAULT, DEFAULT, WORKSPACE);
    }

    private void transformPluginTopic() {
        dmx.getTopicByUri(ZW.ZW_PLUGIN_URI).update(mf.newTopicModel(LQ.LINQA_PLUGIN_URI, PLUGIN,
            mf.newChildTopicsModel()
                .set(PLUGIN_NAME, "DMX Linqa")
                .set(PLUGIN_SYMBOLIC_NAME, LQ.LINQA_PLUGIN_URI)
                .set(PLUGIN_MIGRATION_NR, 2)
        ));
    }
}
