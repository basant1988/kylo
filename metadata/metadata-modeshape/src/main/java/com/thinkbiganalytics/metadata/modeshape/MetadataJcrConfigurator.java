/**
 * 
 */
package com.thinkbiganalytics.metadata.modeshape;

import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.security.Privilege;

import com.thinkbiganalytics.metadata.modeshape.extension.ExtensionsConstants;
import com.thinkbiganalytics.metadata.modeshape.security.AdminCredentials;
import com.thinkbiganalytics.metadata.modeshape.security.JcrAccessControlUtil;

import org.modeshape.jcr.security.SimplePrincipal;

/**
 *
 * @author Sean Felten
 */
public class MetadataJcrConfigurator {
    
    @Inject
    private JcrMetadataAccess metadataAccess;
    
    public void configure() {
        this.metadataAccess.commit(new AdminCredentials(), () -> {
            try {
                Session session = JcrMetadataAccess.getActiveSession();
                
                ensureLayout(session);
                ensureTypes(session);
                ensureAccessControl(session);
                return null;
            } catch (RepositoryException e) {
                throw new MetadataRepositoryException("Could not create initial JCR metadata", e);
            }
        });
    }
    

    private void ensureAccessControl(Session session) {
//        JcrAccessControlUtil.addPermissions(session, "metadata/feeds/test", SimplePrincipal.newInstance("admin"), Privilege.JCR_ALL);
//        JcrAccessControlUtil.addPermissions(session, "metadata/feeds/test/trigger1", SimplePrincipal.newInstance("sean"), Privilege.JCR_ALL);
//        JcrAccessControlUtil.addPermissions(session, "metadata/feeds/test/dependent1", SimplePrincipal.newInstance("dev"), Privilege.JCR_ALL);
//    
//        JcrAccessControlUtil.clearPermissions(session, "metadata/feeds/test");
//        JcrAccessControlUtil.clearPermissions(session, "metadata/feeds/test/trigger1");
//        JcrAccessControlUtil.clearPermissions(session, "metadata/feeds/test/dependent1");
    
    }


    protected void ensureTypes(Session session) throws RepositoryException {
        Node typesNode = session.getRootNode().getNode(ExtensionsConstants.TYPES);
        NodeTypeManager typeMgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator typeItr = typeMgr.getPrimaryNodeTypes();
        NodeType extensionsType = typeMgr.getNodeType(ExtensionsConstants.EXTENSIBLE_ENTITY_TYPE);
        
        while (typeItr.hasNext()) {
            NodeType type = (NodeType) typeItr.next();
            
            if (type.isNodeType(ExtensionsConstants.EXTENSIBLE_ENTITY_TYPE) && 
                            ! type.equals(extensionsType) && 
                            ! typesNode.hasNode(type.getName())) {
                Node descrNode = typesNode.addNode(type.getName(), ExtensionsConstants.TYPE_DESCRIPTOR_TYPE);
                
                descrNode.setProperty("jcr:title", simpleName(type.getName()));
                descrNode.setProperty("jcr:description", "");
                
                PropertyDefinition[] defs = type.getPropertyDefinitions();
                
                for (PropertyDefinition def : defs) {
                    String fieldName = def.getName();
                    String prefix = namePrefix(fieldName);
                    
                    if (! ExtensionsConstants.STD_PREFIXES.contains(prefix) && ! descrNode.hasNode(fieldName)) {
                        Node propNode = descrNode.addNode(def.getName(), ExtensionsConstants.FIELD_DESCRIPTOR_TYPE);
                        propNode.setProperty("jcr:title", def.getName().replace("^.*:", ""));
                        propNode.setProperty("jcr:description", "");
                    }
                }
            }
        }
        
        NodeIterator nodeItr = typesNode.getNodes();
        
        while (nodeItr.hasNext()) {
            Node typeNode = (Node) nodeItr.next();
            
            if (! typeMgr.hasNodeType(typeNode.getName())) {
                typeNode.remove();
            }
        }
    }

    protected void ensureLayout(Session session) throws RepositoryException {
        if (! session.getRootNode().hasNode("metadata")) {
            session.getRootNode().addNode("metadata", "tba:metadataFolder");
        }
        
        // TODO Temporary to cleanup schemas which had the category folder auto-created.
        if (session.getRootNode().hasNode("metadata/feeds/category")) {
            session.getRootNode().getNode("metadata/feeds/category").remove();
        }
    }
    
    private String namePrefix(String name) {
        Matcher m = ExtensionsConstants.NAME_PATTERN.matcher(name);
        
        if (m.matches()) {
            return m.group(1);
        } else {
            return null;
        }
    }
    
    private String simpleName(String name) {
        Matcher m = ExtensionsConstants.NAME_PATTERN.matcher(name);
        
        if (m.matches()) {
            return m.group(2);
        } else {
            return null;
        }
    }
}
