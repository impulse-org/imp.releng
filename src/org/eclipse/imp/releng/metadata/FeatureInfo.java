/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.releng.metadata;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.imp.releng.ReleaseTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Simple class that records key XML metadata about a feature, including its manifest
 * file (as an XML document), the set of required features (represented by the XML
 * document nodes for the corresponding "import" elements), and the set of
 * plugins contained in the feature (represented by the XML document nodes for the
 * corresponding "plugin" element).
 * @author rfuhrer
 */
public class FeatureInfo {
    public final String fFeatureID;

    public final IProject fProject;

    public final IFile fManifestFile;

    /**
     * The XML Document for the feature manifest.
     */
    public Document fManifestDoc;

    /**
     * The set of required features, represented by the XML
     * document nodes for the corresponding "import" elements.
     */
    public Set<Node> fRequiredFeatures= new HashSet<Node>();

    /**
     * The set of plugins contained in the feature (represented by the
     * XML document nodes for the corresponding "plugin" element.
     */
    public Set<Node> fPlugins= new HashSet<Node>();

    public Set<PluginInfo> fPluginInfos= new HashSet<PluginInfo>();

    public String fFeatureVersion;

    private Node fDescriptionNode;

    public FeatureInfo(IProject project, IFile manifestFile) throws ParserConfigurationException, SAXException, IOException {
	fProject= project;
	fManifestFile= manifestFile;

        ReleaseEngineeringPlugin.getMsgStream().println("Got manifest document for " + manifestFile.getLocation().toPortableString());

        DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder= factory.newDocumentBuilder();

        fManifestDoc= docBuilder.parse(new File(manifestFile.getLocation().toOSString()));
	fFeatureID= getFeatureNode().getAttributes().getNamedItem("id").getNodeValue();

        Node featureNode= getFeatureNode();
        Node versionNode= featureNode.getAttributes().getNamedItem("version");
        fDescriptionNode= featureNode.getChildNodes().item(1);
        fFeatureVersion= versionNode.getNodeValue();
    }

    private Node getFeatureVersionNode() {
        return getFeatureNode().getAttributes().getNamedItem("version");
    }

    private Node getFeatureNode() {
        return fManifestDoc.getChildNodes().item(0);
    }

    public void collectPlugins(ReleaseTool incrementer) {
        Node featureNode= getFeatureNode();
        String featureVersion= featureNode.getAttributes().getNamedItem("version").getNodeValue();

        ReleaseEngineeringPlugin.getMsgStream().println("Processing feature " + featureNode.getAttributes().getNamedItem("id") + " version " + featureVersion + " from " + fManifestFile.getLocation().toPortableString());

        Set<Node> featureRequires= ReleaseTool.getChildNodesNamed("requires", featureNode);
        Set<Node> featurePlugins= ReleaseTool.getChildNodesNamed("plugin", featureNode);

        for(Node required: featureRequires) {
            Set<Node> imports= ReleaseTool.getChildNodesNamed("import", required);
            for(Node imprt: imports) {
                if (imprt.getAttributes().getNamedItem("feature") != null) {
                    ReleaseEngineeringPlugin.getMsgStream().println("   requires " + imprt.getAttributes().getNamedItem("feature") + " version " + imprt.getAttributes().getNamedItem("version"));
                    fRequiredFeatures.add(imprt);
                } else if (imprt.getAttributes().getNamedItem("plugin") != null) {
//                    VersionIncrementerPlugin.getMsgStream().println("   requires " + imprt.getAttributes().getNamedItem("plugin") + " version " + imprt.getAttributes().getNamedItem("version"));
                }
            }
        }
        for(Node plugin: featurePlugins) {
            String pluginID= plugin.getAttributes().getNamedItem("id").getNodeValue();
            String pluginVersion= plugin.getAttributes().getNamedItem("version").getNodeValue();

            ReleaseEngineeringPlugin.getMsgStream().println("   contains " + pluginID + " version " + pluginVersion);
            fPlugins.add(plugin);
            PluginInfo pluginInfo= new PluginInfo(pluginID, pluginVersion);
            fPluginInfos.add(pluginInfo);
            incrementer.addPlugin(pluginInfo);
        }
    }

    public void setNewVersion(String newVersion) {
        getFeatureVersionNode().setNodeValue(newVersion);
    }

    public String getURL() {
        return fDescriptionNode.getAttributes().getNamedItem("url").getNodeValue();
    }

    @Override
    public int hashCode() {
        return 3847 + 5779 * fFeatureID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeatureInfo)) return false;
        FeatureInfo other= (FeatureInfo) obj;
        return fFeatureID.equals(other.fFeatureID);
    }

    @Override
    public String toString() {
        return "<feature " + fFeatureID + " version " + fFeatureVersion + ">";
    }
}
