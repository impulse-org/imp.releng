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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FileVersionMap {
    private IProject fProject;

    private String fPluginID;

    private String fVersion;

    private Map<IFile, String> fMap= new HashMap<IFile, String>();

    public FileVersionMap(IProject project, String pluginID, String version) {
        fProject= project;
        fPluginID= pluginID;
        fVersion= version;
        IFile mapFile= fProject.getFile(pathToMap());

        if (!mapFile.exists()) {
            // VersionIncrementerPlugin.getMsgStream().println("No existing version map file for " + fProject.getName());
            // presumably the client will fill in the map
        } else {
            try {
                DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder= factory.newDocumentBuilder();
                Document mapDoc= docBuilder.parse(new File(mapFile.getLocation().toOSString()));

                // build up map from mapDoc
                Node mapDocElem= mapDoc.getDocumentElement();
                // String mapPluginID= mapDocElem.getAttributes().getNamedItem("pluginID").getNodeValue();
                // String mapPluginVersion=
                // mapDocElem.getAttributes().getNamedItem("versionID").getNodeValue();

                NodeList childNodes= mapDocElem.getChildNodes();
                for(int i= 0; i < childNodes.getLength(); i++) {
                    Node child= childNodes.item(i);

                    if (child.getNodeName().equals("file")) {
                        String filePath= child.getAttributes().getNamedItem("path").getNodeValue();
                        String fileVersion= child.getAttributes().getNamedItem("version").getNodeValue();
                        IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filePath));

                        setVersion(file, fileVersion);
                    }
                }
            } catch (IOException e) {
                ReleaseEngineeringPlugin.logError(e);
            } catch (SAXException e) {
                ReleaseEngineeringPlugin.logError(e);
            } catch (ParserConfigurationException e) {
                ReleaseEngineeringPlugin.logError(e);
            }
        }
    }

    public void setVersion(IFile f, String version) {
        fMap.put(f, version);
    }

    public String getVersion(IFile f) {
        return fMap.get(f);
    }

    public Set<IFile> getAllFiles() {
        return fMap.keySet();
    }

    public Set<IFile> getAllFilesSorted() {
        Set<IFile> allFiles= new TreeSet<IFile>(new Comparator<IFile>() {
            public int compare(IFile arg0, IFile arg1) {
                return arg0.getLocation().toPortableString().compareTo(arg1.getLocation().toPortableString());
            }
        });
        allFiles.addAll(fMap.keySet());
        return allFiles;
    }

    public boolean isEmpty() {
        return fMap.isEmpty();
    }

    /**
     * @return a project-relative path to the map file
     */
    private IPath pathToMap() {
        String vers= fVersion.endsWith(".qualifier") ? fVersion.substring(0, fVersion.indexOf(".qualifier")) : fVersion;

        return pathToMap(vers);
    }

    /**
     * @return a project-relative path to the map file for the given version
     */
    private IPath pathToMap(String version) {
        return new Path("releng/versionMap-" + version + ".txt");
    }

    public IFile writeMap() {
        IFile file= fProject.getFile(pathToMap());

        writeMapToFile(file);
        return file;
    }

    private void writeMapToFile(IFile file) {
        if (!file.getParent().exists()) {
            IFolder parent= (IFolder) file.getParent();
            try {
                parent.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                ReleaseEngineeringPlugin.logError(e);
            }
        }

        File f= new File(file.getLocation().toOSString());
        try {
            FileWriter fw= new FileWriter(f, false);

            fw.append(this.toString());
            fw.flush();
            fw.close();
        } catch (IOException e) {
            ReleaseEngineeringPlugin.logError(e);
        }
    }

    private final String lineTerm= "\n";

    public String toString() {
        StringBuilder sb= new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineTerm);
        sb.append("<version pluginID=\"" + fPluginID + "\"" + lineTerm + "         versionID=\"" + fVersion + "\">" + lineTerm);
        Set<IFile> allFiles= getAllFilesSorted();
        for(IFile file : allFiles) {
            sb.append("    <file path=\"" + file.getFullPath().toPortableString() + "\"" + lineTerm + "          version=\"" + fMap.get(file) + "\"/>" + lineTerm);
        }
        sb.append("</version>" + lineTerm);
        return sb.toString();
    }
}
