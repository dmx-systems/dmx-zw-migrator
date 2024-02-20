package systems.dmx.zwmigrator;

import static systems.dmx.accesscontrol.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.files.Constants.*;
import static systems.dmx.topicmaps.Constants.*;
import static systems.dmx.workspaces.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.workspaces.WorkspacesService;
import systems.dmx.zwmigrator.LQ;
import systems.dmx.zwmigrator.ZW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;



/**
 * Transforms a Zukunftswerk instance into a Linqa instance.
 */
public class ZWMigratorThread extends Thread {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WorkspacesService wss;
    private CoreService dmx;
    private ModelFactory mf;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ZWMigratorThread(WorkspacesService wss, CoreService dmx, ModelFactory mf) {
        this.wss = wss;
        this.dmx = dmx;
        this.mf = mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("### Starting ZW->Linqa migration (in background) ###");
        //
        long comments   = retypeBilingualAssocs("comment");
        long documents  = retypeBilingualAssocs("document", "document_name");
        long notes      = retypeBilingualAssocs("note");
        long textblocks = retypeBilingualAssocs("textblock");
        long headings   = retypeBilingualAssocs("label");
        //
        retypeTopics("comment");
        retypeTopics("comment.de", "comment_text");
        retypeTopics("comment.fr", "comment_text");
        retypeTopics("document");
        retypeTopics("document_name.de", "document_name");
        retypeTopics("document_name.fr", "document_name");
        retypeTopics("note");
        retypeTopics("note.de", "note_text");
        retypeTopics("note.fr", "note_text");
        retypeTopics("textblock");
        retypeTopics("textblock.de", "textblock_text");
        retypeTopics("textblock.fr", "textblock_text");
        retypeTopics("label", "heading");
        retypeTopics("label.de", "heading_text");
        retypeTopics("label.fr", "heading_text");
        //
        long arrows = retypeTopics("arrow");
        retypeTopics("viewport");
        retypeTopics("language");
        retypeTopics("translation_edited");
        retypeTopics("locked");
        retypeTopics("editor");
        retypeTopics("show_email_address");
        //
        retypeAssocs("shared_workspace");
        retypeAssocs("attachment");
        retypeAssocs("original_language");
        retypeAssocs("de", "lang1");
        retypeAssocs("fr", "lang2");
        //
        long workspaces = transformProperties();
        transformAdminProperties();
        transformTeamWorkspace();
        transformWorkspaceModel();
        transformPluginTopic();
        //
        deleteZukunftswerkModel();
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

    private long retypeBilingualAssocs(String item) {
        return retypeBilingualAssocs(item, null);
    }

    /**
     * @param   item        URI fragment (mandatory)
     * @param   biItem      URI fragment (optional), used for accessing the bilingual child value.
     *                      If null "item" is used.
     */
    private long retypeBilingualAssocs(String item, String biItem) {
        return tx(() -> {
            return dmx.getTopicsByType("zukunftswerk." + item).stream().filter(topic -> {
                String _biItem = biItem != null ? biItem : item;
                RelatedTopic de = getRelatedTopic(topic, "zukunftswerk." + _biItem + ".de");
                RelatedTopic fr = getRelatedTopic(topic, "zukunftswerk." + _biItem + ".fr");
                if (de != null) {
                    de.getRelatingAssoc().setTypeUri(LQ.LANG1);
                }
                if (fr != null) {
                    fr.getRelatingAssoc().setTypeUri(LQ.LANG2);
                }
                return true;
            }).count();
        });
    }

    // Note: we can't use model-driven child topic access, in case of shared child topics it would fail.
    // So we use manual DB navigation.
    private RelatedTopic getRelatedTopic(Topic topic, String childTypeUri) {
        return topic.getRelatedTopic(COMPOSITION, PARENT, CHILD, childTypeUri);
    }

    private long retypeTopics(String item) {
        return retypeTopics(item, null);
    }

    private long retypeTopics(String item, String targetItem) {
        return tx(() -> {
            String typeUri = "linqa." + (targetItem != null ? targetItem : item);
            return dmx.getTopicsByType("zukunftswerk." + item).stream().filter(topic -> {
                topic.setTypeUri(typeUri);
                return true;
            }).count();
        });
    }

    private long retypeAssocs(String item) {
        return retypeAssocs(item, null);
    }

    private long retypeAssocs(String item, String targetItem) {
        return tx(() -> {
            String typeUri = "linqa." + (targetItem != null ? targetItem : item);
            return dmx.getAssocsByType("zukunftswerk." + item).stream().filter(assoc -> {
                assoc.setTypeUri(typeUri);
                return true;
            }).count();
        });
    }

    private long transformProperties() {
        return forAllWorkspaces(ws -> {
            tx(() -> {
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
                    transformProperty(assoc, ZW.ZW_COLOR, LQ.LINQA_COLOR);
                    transformProperty(assoc, ZW.ANGLE, LQ.ANGLE);
                });
                return null;
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

    private void transformAdminProperties() {
        tx(() -> {
            dmx.getTopicsByType(USERNAME).stream().forEach(topic -> {
                transformProperty(topic, ZW.USER_ACTIVE, LQ.USER_ACTIVE);
            });
            return null;
        });
    }

    private void transformProperty(DMXObject object, String srcProp, String targetProp) {
        if (object.hasProperty(srcProp)) {
            object.setProperty(targetProp, object.getProperty(srcProp), false);    // addToIndex=false
            object.removeProperty(srcProp);
        }
    }

    private void transformTeamWorkspace() {
        tx(() -> {
            dmx.getTopicByUri(ZW.TEAM_WORKSPACE_URI).setUri(LQ.LINQA_ADMIN_WS_URI);
            return null;
        });
    }

    private void transformWorkspaceModel() {
        tx(() -> {
            // "Workspace Name" type def
            TopicType type = dmx.getTopicType(WORKSPACE);
            String compDefUri = ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE;
            type.getCompDef(WORKSPACE_NAME + "#" + ZW.DE).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG1));
            type.getCompDef(WORKSPACE_NAME + "#" + ZW.FR).update(mf.newChildTopicsModel().setRef(compDefUri, LQ.LANG2));
            return null;
        });
    }

    private void transformPluginTopic() {
        tx(() -> {
            dmx.getTopicByUri(ZW.ZW_PLUGIN_URI).update(mf.newTopicModel(LQ.LINQA_PLUGIN_URI, PLUGIN,
                mf.newChildTopicsModel()
                    .set(PLUGIN_NAME, "DMX Linqa")
                    .set(PLUGIN_SYMBOLIC_NAME, LQ.LINQA_PLUGIN_URI)
                    .set(PLUGIN_MIGRATION_NR, 2)
            ));
            return null;
        });
    }

    private void deleteZukunftswerkModel() {
        //
        deleteTopicType(ZW.DOCUMENT);
        deleteTopicType(ZW.DOCUMENT_NAME_DE);
        deleteTopicType(ZW.DOCUMENT_NAME_FR);
        deleteTopicType(ZW.ZW_NOTE);
        deleteTopicType(ZW.ZW_NOTE_DE);
        deleteTopicType(ZW.ZW_NOTE_FR);
        deleteTopicType(ZW.TEXTBLOCK);
        deleteTopicType(ZW.TEXTBLOCK_DE);
        deleteTopicType(ZW.TEXTBLOCK_FR);
        deleteTopicType(ZW.LABEL);
        deleteTopicType(ZW.LABEL_DE);
        deleteTopicType(ZW.LABEL_FR);
        deleteTopicType(ZW.ARROW);
        deleteTopicType(ZW.COMMENT);
        deleteTopicType(ZW.COMMENT_DE);
        deleteTopicType(ZW.COMMENT_FR);
        deleteTopicType(ZW.LANGUAGE);
        deleteTopicType(ZW.TRANSLATION_EDITED);
        deleteTopicType(ZW.LOCKED);
        deleteTopicType(ZW.VIEWPORT);
        deleteTopicType(ZW.EDITOR);
        deleteTopicType(ZW.EDITOR_FACET);
        deleteTopicType(ZW.SHOW_EMAIL_ADDRESS);
        deleteTopicType(ZW.SHOW_EMAIL_ADDRESS_FACET);
        //
        deleteAssocType(ZW.SHARED_WORKSPACE);
        deleteAssocType(ZW.ATTACHMENT);
        deleteAssocType(ZW.ORIGINAL_LANGUAGE);
        deleteAssocType(ZW.DE);
        deleteAssocType(ZW.FR);
    }

    private void deleteTopicType(String typeUri) {
        tx(() -> {
            dmx.deleteTopicType(typeUri);
            return null;
        });
    }

    private void deleteAssocType(String typeUri) {
        tx(() -> {
            dmx.deleteAssocType(typeUri);
            return null;
        });
    }

    private <T> T tx(Supplier<T> body) {
        DMXTransaction tx = dmx.beginTx();
        try {
            T result = body.get();
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }
}
