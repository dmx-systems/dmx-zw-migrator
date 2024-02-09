package systems.dmx.zwmigrator.migrations;

import static systems.dmx.core.Constants.*;
import static systems.dmx.topicmaps.Constants.*;
import static systems.dmx.workspaces.Constants.*;
import static systems.dmx.files.Constants.*;
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
        long comments   = retypeBilingualTopics("comment");
        long documents  = retypeBilingualTopics("document", "document_name", null);
        long notes      = retypeBilingualTopics("note");
        long textblocks = retypeBilingualTopics("textblock");
        long headings   = retypeBilingualTopics("label", null, "heading");
        //
        long arrows = retypeTopics("arrow");
        retypeTopics("viewport");
        retypeTopics("language");
        retypeTopics("translation_edited");
        retypeTopics("locked");
        //
        retypeAssocs("shared_workspace");
        retypeAssocs("attachment");
        retypeAssocs("original_language");
        //
        long workspaces = transformProperties();
        transformTeamWorkspace();
        transformWorkspaceModel();
        transformPluginTopic();
        //
        retypeAssocs("de", "lang1");
        retypeAssocs("fr", "lang2");
        //
        logger.info("##### ZW->Linqa migration complete #####\n  " +
            "Workspaces: " + workspaces + "\n  " +
            "Comments: "   + comments + "\n  " +
            "Documents: "  + documents + "\n  " +
            "Notes: "      + notes + "\n  " +
            "Textblocks: " + textblocks + "\n  " +
            "Headings: "   + headings + "\n  " +
            "Arrows: "     + arrows
        );
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private long retypeBilingualTopics(String item) {
        return retypeBilingualTopics(item, null, null);
    }

    /**
     * @param   item        URI fragment (mandatory)
     * @param   biItem      URI fragment (optional, may be null), used in 2 contexts:
     *                      1) accessing the bilingual child value. If null "item" is used.
     *                      2) calculating the target type of the bilingual child value.
     *                         If null "item" or "targetItem" (if given) is used, appended by "_text".
     */
    private long retypeBilingualTopics(String item, String biItem, String targetItem) {
        return dmx.getTopicsByType("zukunftswerk." + item).stream().filter(topic -> {
            // bilingual text
            String _biItem = biItem != null ? biItem : item;
            RelatedTopic de = getRelatedTopic(topic, "zukunftswerk." + _biItem + ".de");
            RelatedTopic fr = getRelatedTopic(topic, "zukunftswerk." + _biItem + ".fr");
            String _targetBiItem = "linqa." + (biItem != null ? biItem :
                                              (targetItem != null ? targetItem : item) + "_text");
            if (de != null) {
                de.setTypeUri(_targetBiItem);
                de.getRelatingAssoc().setTypeUri(LQ.LANG1);
            }
            if (fr != null) {
                fr.setTypeUri(_targetBiItem);
                fr.getRelatingAssoc().setTypeUri(LQ.LANG2);
            }
            // retype composite
            String _targetItem = targetItem != null ? targetItem : item;
            topic.setTypeUri("linqa." + _targetItem);
            return true;
        }).count();
    }

    // Note: we can't use model-driven child topic access, in case of shared child topics it would fail.
    // So we use manual DB navigation.
    private RelatedTopic getRelatedTopic(Topic topic, String childTypeUri) {
        return topic.getRelatedTopic(COMPOSITION, PARENT, CHILD, childTypeUri);
    }

    private long retypeTopics(String item) {
        return dmx.getTopicsByType("zukunftswerk." + item).stream().filter(topic -> {
            topic.setTypeUri("linqa." + item);
            return true;
        }).count();
    }

    private long retypeAssocs(String item) {
        return retypeAssocs(item, null);
    }

    private long retypeAssocs(String item, String targetItem) {
        String typeUri = "linqa." + (targetItem != null ? targetItem : item);
        return dmx.getAssocsByType("zukunftswerk." + item).stream().filter(assoc -> {
            assoc.setTypeUri(typeUri);
            return true;
        }).count();
    }

    private long transformProperties() {
        return forAllWorkspaces(ws -> {
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

    private long forAllWorkspaces(Consumer<Topic> consumer) {
        consumer.accept(dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI));
        return getAllZWWorkspaces().stream().filter(ws -> {
            consumer.accept(ws);
            return true;
        }).count() + 1;
    }

    private List<RelatedTopic> getAllZWWorkspaces() {
        return dmx.getTopicByUri(ZW.ZW_PLUGIN_URI).getRelatedTopics(ZW.SHARED_WORKSPACE, DEFAULT, DEFAULT, WORKSPACE);
    }

    private void transformTeamWorkspace() {
        // "Team" workspace: rename + change URI            // FIXME: do renaming actually?
        dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI).update(
            mf.newTopicModel(LQ.LINQA_ADMIN_WS_URI, WORKSPACE, mf.newChildTopicsModel()
                .set(WORKSPACE_NAME, LQ.LINQA_ADMIN_WS_NAME)
                .set(WORKSPACE_NAME + "#" + ZW.DE, LQ.LINQA_ADMIN_WS_NAME)
            )
        );
    }

    private void transformWorkspaceModel() {
        // "Workspace Name" type def
        TopicType type = dmx.getTopicType(WORKSPACE);
        String compDefUri = ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE;
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.DE).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG1));
        type.getCompDef(WORKSPACE_NAME + "#" + ZW.FR).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG2));
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
