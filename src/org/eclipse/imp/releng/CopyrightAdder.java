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

package org.eclipse.imp.releng;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.imp.releng.dialogs.SelectFeatureInfosDialog;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.PlatformUI;

public class CopyrightAdder {
    private interface ICopyrightAdder {
        String addCopyright(String src);
    }

    private class JavaEPLCopyrightAdder implements ICopyrightAdder {
        private static final String COPYRIGHT_NOTICE= 
                "/*******************************************************************************\n" +
                "* Copyright (c) 2008 IBM Corporation.\n" +
                "* All rights reserved. This program and the accompanying materials\n" +
                "* are made available under the terms of the Eclipse Public License v1.0\n" +
                "* which accompanies this distribution, and is available at\n" +
                "* http://www.eclipse.org/legal/epl-v10.html\n" +
                "*\n" +
                "* Contributors:\n" +
                "*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation\n" +
                "\n" +
                "*******************************************************************************/\n\n";
        
        private static final String OLD_COPYRIGHT_NOTICE= "/*\r\n" +
            " * (C) Copyright IBM Corporation 2007\r\n" +
            " * \r\n" +
            " * This file is part of the Eclipse IMP.\r\n" +
            " */\r\n";
        
        private final int OLD_COPYRIGHT_LENGTH = OLD_COPYRIGHT_NOTICE.length();
        
        public String removeOldCopyright(String src) {
                int oldStart = src.indexOf(OLD_COPYRIGHT_NOTICE);
                if (oldStart < 0)
                        return src;
                if (oldStart == 0)
                        return src.substring(OLD_COPYRIGHT_LENGTH);
                String prefix = src.substring(0, oldStart);
                String suffix = src.substring(oldStart + OLD_COPYRIGHT_LENGTH);
                return prefix + suffix;
        }
        
        
        public String addCopyright(String src) {
            if (src.contains(COPYRIGHT_NOTICE))
                return src;
            src = removeOldCopyright(src);
                return COPYRIGHT_NOTICE + src;
        }
    }

    private class LPGEPLCopyrightAdder implements ICopyrightAdder {
        private static final String COPYRIGHT_NOTICE = 
        "%Notice\n" +
        "/.\n" +
        "////////////////////////////////////////////////////////////////////////////////\n" +
        "// Copyright (c) 2007 IBM Corporation.\n" +
        "// All rights reserved. This program and the accompanying materials\n" +
        "// are made available under the terms of the Eclipse Public License v1.0\n" +
        "// which accompanies this distribution, and is available at\n" +
        "// http://www.eclipse.org/legal/epl-v10.html\n" +
        "//\n" +
        "//Contributors:\n" +
        "//    Philippe Charles (pcharles@us.ibm.com) - initial API and implementation\n" +
        "\n" +
        "////////////////////////////////////////////////////////////////////////////////\n" +
        "./\n" +
        "%End\n" +
        "\n";

        
        private static final String OLD_COPYRIGHT_NOTICE= "%Notice\r\n" +
            "/.\r\n" +
            "// (C) Copyright IBM Corporation 2007\r\n" +
            "// \r\n" +
            "// This file is part of the Eclipse IMP.\r\n" +
            "./\r\n" +
            "%End\r\n" +
            "\r\n";
        
        private final int OLD_COPYRIGHT_LENGTH = OLD_COPYRIGHT_NOTICE.length();
        
        public String removeOldCopyright(String src) {
                int oldStart = src.indexOf(OLD_COPYRIGHT_NOTICE);
                if (oldStart < 0)
                        return src;
                if (oldStart == 0)
                        return src.substring(OLD_COPYRIGHT_LENGTH);
                String prefix = src.substring(0, oldStart);
                String suffix = src.substring(oldStart + OLD_COPYRIGHT_LENGTH);
                return prefix + suffix;
        }
        
        public String addCopyright(String src) {
            if (src.contains(COPYRIGHT_NOTICE))
                return src;
            src = removeOldCopyright(src);
            int rulesLoc= src.indexOf("%Rules"); // Look for the new keyword form ("%Rules") first; "XXX$Rules" is a legal right-hand side symbol reference
            if (rulesLoc < 0)
                rulesLoc= src.indexOf("$Rules");
            return src.substring(0, rulesLoc) + COPYRIGHT_NOTICE + src.substring(rulesLoc);
        }
    }

    private ReleaseTool fReleaseTool= new WorkbenchReleaseTool();

    private Map<String,ICopyrightAdder> fExtensionMap= new HashMap<String,ICopyrightAdder>();

    {
        LPGEPLCopyrightAdder lpgAdder= new LPGEPLCopyrightAdder();

        fExtensionMap.put("g", lpgAdder);
        fExtensionMap.put("gi", lpgAdder);
        fExtensionMap.put("java", new JavaEPLCopyrightAdder());
    }

    private final Set<IFolder> fSrcRoots= new HashSet<IFolder>();

    public void addCopyrights() {
//        final IPreferenceStore prefStore= ReleaseEngineeringPlugin.getInstance().getPreferenceStore();

        fReleaseTool.collectMetaData(true);

        fSrcRoots.clear();

        List<FeatureInfo> featureInfos= fReleaseTool.getFeatureInfos();

        SelectFeatureInfosDialog sfid= new SelectFeatureInfosDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), featureInfos);

        if (sfid.open() != Dialog.OK) {
            return;
        }

        IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
        Set<IProject> projects= new HashSet<IProject>();

        for(FeatureInfo featureInfo: sfid.getSelectedFeatures()) {
            for(PluginInfo plugin: featureInfo.fPluginInfos) {
                projects.add(wsRoot.getProject(plugin.fProjectName));
            }
        }

        collectProjectSourceRoots(projects);

        for(IFolder srcRoot: fSrcRoots) {
            try {
                srcRoot.accept(new IResourceVisitor() {
                    public boolean visit(IResource resource) throws CoreException {
                        if (resource.isDerived())
                            return false;

                        if (resource instanceof IFile) {
                            IFile file= (IFile) resource;
                            String fileExtension= file.getFileExtension();

                            if (fExtensionMap.containsKey(fileExtension)) {
                                ICopyrightAdder copyrightAdder= fExtensionMap.get(fileExtension);

                                addCopyright(copyrightAdder, file);
                            }
                        } else if (resource instanceof IFolder) {
                            ReleaseEngineeringPlugin.getMsgStream().println("  Scanning folder " + resource.getLocation().toPortableString());
                        }
                        return true;
                    }

                    private void addCopyright(ICopyrightAdder copyrightAdder, IFile file) {
                        try {
                            ReleaseEngineeringPlugin.getMsgStream().println("    Processing file " + file.getLocation().toPortableString());
                            InputStream is= file.getContents();
                            String origCont= getFileContents(new InputStreamReader(is));

                            final String newCont= copyrightAdder.addCopyright(origCont);

                            file.setContents(new InputStream() {
                                private int idx= 0;
                                @Override
                                public int read() throws IOException {
                                    if (idx < newCont.length())
                                        return newCont.charAt(idx++);
                                    return -1;
                                }
                            }, true, true, new NullProgressMonitor());
                        } catch(CoreException e) {
                            logError(e);
                            e.printStackTrace();
                        }
                    }
                });
            } catch (CoreException e) {
                logError(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * @param projects
     */
    private void collectProjectSourceRoots(Set<IProject> projectsToModify) {
        IWorkspaceRoot wsRoot= fReleaseTool.fWSRoot;

        for(IProject project: projectsToModify) {
            ReleaseEngineeringPlugin.getMsgStream().println("Collecting source folders for " + project.getName());

            IJavaProject javaProject= JavaCore.create(project);

            if (!javaProject.exists()) {
                ReleaseEngineeringPlugin.getMsgStream().println("Project " + javaProject.getElementName() + " does not exist!");
                continue;
            }

            try {
                IClasspathEntry[] cpe= javaProject.getResolvedClasspath(true);

                for(int j= 0; j < cpe.length; j++) {
                    IClasspathEntry entry= cpe[j];
                    if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                        // TODO Doesn't handle path inclusion/exclusion constraints
                        if (entry.getPath().segmentCount() == 1)
                            ReleaseEngineeringPlugin.getMsgStream().println("*** Ignoring source path entry " + entry.getPath().toPortableString() + " because it's at a project root.");
                        else
                            fSrcRoots.add(wsRoot.getFolder(entry.getPath()));
                    }
                }
            } catch (JavaModelException e) {
                ReleaseEngineeringPlugin.getMsgStream().println("Exception encountered while traversing resources:\n  " + e.getMessage());
                logError(e);
            }
        }
    }

    public static String getFileContents(Reader reader) {
        // In this case we don't know the length in advance, so we have to
        // accumulate the reader's contents one buffer at a time.
        StringBuilder sb= new StringBuilder(4096);
        char[] buff= new char[4096];
        int len;

        while(true) {
            try {
                len= reader.read(buff);
            } catch (IOException e) {
                break;
            }
            if (len < 0)
                break;
            sb.append(buff, 0, len);
        }
        return sb.toString();
    }

    private void logError(Exception e) {
        final Status status= new Status(IStatus.ERROR, ReleaseEngineeringPlugin.kPluginID, 0, e.getMessage(), e);
        ReleaseEngineeringPlugin.getInstance().getLog().log(status);
    }
}
