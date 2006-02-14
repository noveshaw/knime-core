/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (${user}): created
 */
package de.unikn.knime.workbench.repository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import de.unikn.knime.workbench.repository.model.Category;
import de.unikn.knime.workbench.repository.model.IContainerObject;
import de.unikn.knime.workbench.repository.model.IRepositoryObject;
import de.unikn.knime.workbench.repository.model.NodeTemplate;
import de.unikn.knime.workbench.repository.model.Root;

/**
 * Factory for creation of repository objects from
 * <code>IConfigurationElement</code> s from the Plugin registry.
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class RepositoryFactory {

    private RepositoryFactory() {
        // hidden constructor (utility class)
    }

    /**
     * Creates a new node repository object. Throws an exception, if this fails
     * 
     * @param element Configuration element from the contributing plugin
     * @return NodeTemplate object to be used within the repository.
     * @throws IllegalArgumentException If the element is not compatible (e.g.
     *             wrong attributes, or factory class not found)
     */
    public static NodeTemplate createNode(final IConfigurationElement element) {
        String id = element.getAttribute("id");

        NodeTemplate node = new NodeTemplate(id);

        node.setDescription(str(element.getAttribute("description"), ""));
        node.setName(str(element.getAttribute("name"), "!name is missing!"));
        node
                .setType(str(element.getAttribute("type"),
                        NodeTemplate.TYPE_OTHER));

        String icon = str(element.getAttribute("icon"), "");
        // TODO FIXME compatibility (old name): to be removed !!
        if (icon.equals("")) {
            icon = str(element.getAttribute("icon-small"), "");
        }
        node.setIconPath(icon);

        // Load images from declaring plugin
        String pluginID = element.getDeclaringExtension().getNamespace();
        node.setPluginID(pluginID);

        // FIXME dispose this somewhere !!
        node.setIcon(HadesRepositoryPlugin.getDefault()
                .getImage(pluginID, icon));
        node.setIconDescriptor(HadesRepositoryPlugin.getDefault()
                .getImageDescriptor(pluginID, icon));

        node.setCategoryPath(str(element.getAttribute("category-path"), "/"));

        // Try to load the node factory class...
        try {
            Object factory;
            // this ensures that the class is loaded by the correct eclipse
            // classloaders
            factory = element.createExecutableExtension("factory-class");

            node.setFactory(factory.getClass());
        } catch (CoreException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    "Can't load factory class for node: "
                            + element.getAttribute("factory-class"));

        }

        return node;
    }

    /**
     * Creates a new category object. Throws an exception, if this fails
     * 
     * @param root The root to insert the category in
     * @param element Configuration element from the contributing plugin
     * @return Category object to be used within the repository.
     * @throws IllegalArgumentException If the element is not compatible (e.g.
     *             wrong attributes)
     */
    public static Category createCategory(final Root root,
            final IConfigurationElement element) {

        String id = element.getAttribute("level-id");

        // get the id of the contributing plugin
        String pluginID = element.getDeclaringExtension().getNamespace();

        Category cat = new Category(id);
        cat.setDescription(str(element.getAttribute("description"), ""));
        cat.setName(str(element.getAttribute("name"), "!name is missing!"));
        cat.setAfterID(str(element.getAttribute("after"), ""));
        cat.setIcon(HadesRepositoryPlugin.getDefault().getImage(pluginID,
                str(element.getAttribute("icon"), "")));
        cat.setIconDescriptor(HadesRepositoryPlugin.getDefault()
                .getImageDescriptor(pluginID,
                        str(element.getAttribute("icon"), "")));

        String path = str(element.getAttribute("path"), "/");
        cat.setPath(path);

        //
        // Insert in proper location, create all categories on the path
        // if not already there
        //
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // split the path
        String[] segments = path.split("/");
        // path so far
        String pathSoFar = "";
        // start at root
        IContainerObject container = root;
        IContainerObject child = null;
        for (int i = 0; i < segments.length; i++) {

            pathSoFar += "/" + segments[i];
            // look, if the container already exists. If not, it will be
            // created with the segment as the ID.
            // Note that several properties as icon,description are
            // set to a default.
            IRepositoryObject obj = container.getChildByID(segments[i], true);
            if (obj == null) {
                continue;
            }

            child = (IContainerObject) obj;

            // if we have found a category (root will be skipped) ....
            if (child instanceof Category) {
                Category category = (Category) child;
                if (category == null) {
                    // ASSERT: the segment is not empty
                    assert (segments[i] != null)
                            && (!segments[i].trim().equals(""));

                    // 
                    // Create a new category, set all fields to defaults where
                    // appropriate.

                    // the segment is the id of this new category
                    category = new Category(segments[i]);
                    category.setName(segments[i]);
                    category.setPath(pathSoFar);
                    // this loads the default icon
                    category.setIcon(HadesRepositoryPlugin.getDefault()
                            .getImage(pluginID, ""));

                    // add this category to the current container
                    container.addChild(category);
                }
            }
            // continue at this level
            container = child;
        }

        // append the newly created category to the container
        container.addChild(cat);

        return cat;
    }

    //
    // little helper, returns a default if s==null
    private static String str(final String s, final String defaultString) {
        return s == null ? defaultString : s;
    }

}
