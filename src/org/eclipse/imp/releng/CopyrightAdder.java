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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.imp.releng.dialogs.ConfirmChangedFileViewer;
import org.eclipse.imp.releng.dialogs.SelectFeatureInfosDialog;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.PlatformUI;

public class CopyrightAdder {
    private static final class ConfirmChangedFilesDialog extends Dialog {
    	private final CompositeChange fTopChange;

		private ConfirmChangedFilesDialog(Shell parentShell, CompositeChange topChange) {
			super(parentShell);
			fTopChange= topChange;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Add copyright changes");
		}

		@Override
		protected Point getInitialSize() {
			return new Point(750, 350);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite dialogArea= (Composite) super.createDialogArea(parent);
			Composite composite= new Composite(dialogArea, SWT.NONE);
		    GridData gd= new GridData(GridData.FILL, GridData.FILL, true, true);
		    gd.heightHint= 270;
		    gd.widthHint= 720;
		    composite.setLayoutData(gd);
		    GridLayout layout= new GridLayout();
		    composite.setLayout(layout);

		    final Tree chgTreeControl= createChangeTreeViewer(composite);

		    gd= new GridData(GridData.FILL_BOTH);
		    gd.heightHint= 120;
		    gd.widthHint= 720;
		    chgTreeControl.setLayoutData(gd);

		    final ConfirmChangedFileViewer v= new ConfirmChangedFileViewer();
		    v.createControl(composite);
		    gd= new GridData(GridData.FILL_BOTH);
		    gd.heightHint= 150;
		    gd.widthHint= 720;
		    v.getControl().setLayoutData(gd);

		    setupSelectionListener(chgTreeControl, v);
		    return dialogArea;
		}

		private void setupSelectionListener(final Tree chgTree, final ConfirmChangedFileViewer v) {
			chgTree.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) { }
				public void widgetSelected(SelectionEvent e) {
					TextFileChange tfc= (TextFileChange) e.item.getData();

					v.setInput(ConfirmChangedFileViewer.createInput(tfc));
				}
		    });
		}

		private Tree createChangeTreeViewer(Composite parent) {
			Tree tree= new Tree(parent, SWT.SINGLE);
			populateSubTree(tree, fTopChange);
			return tree;
		}

		private void populateSubTree(Tree parent, Change change) {
			if (change instanceof CompositeChange) {
				CompositeChange compChange= (CompositeChange) change;

				for(Change child: compChange.getChildren()) {
					TreeItem newItem= new TreeItem(parent, SWT.NONE);
					newItem.setText(child.getName());
					newItem.setData(child);
					populateSubTree(newItem, child);
				}
			}
		}

		private void populateSubTree(TreeItem parentItem, Change change) {
			if (change instanceof CompositeChange) {
				CompositeChange compChange= (CompositeChange) change;

				for(Change child: compChange.getChildren()) {
					TreeItem newItem= new TreeItem(parentItem, SWT.NONE);
					newItem.setText(child.getName());
					newItem.setData(child);
					populateSubTree(newItem, child);
				}
			}
		}
	}

	private interface ICopyrightAdder {
        String addCopyright(String src);
    }

    private class JavaEPLCopyrightAdder implements ICopyrightAdder {
    	private Pattern COPYRIGHT_PATTERN= Pattern.compile(
    			"/\\*.*\n" +
                "\\s*\\* Copyright \\(c\\) [0-9]+ IBM Corporation.\n" +
                "\\s*\\* All rights reserved. This program and the accompanying materials\n" +
                "\\s*\\* are made available under the terms of the Eclipse Public License v1.0\n" +
                "\\s*\\* which accompanies this distribution, and is available at\n" +
                "\\s*\\* http://www.eclipse.org/legal/epl-v10.html\n" +
                "\\s*\\*\n" +
                "\\s*\\* Contributors:\n" +
                "(\\s*\\*.*\n)+" + // 1 or more contributor lines
                "\\s*\\*?\n" +
                ".*\\*/\n");

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
            "*\n" +
            "*******************************************************************************/\n\n";

        /**
         * This is the old, pre-eclipse.org copyright notice that needs to be replaced.
         */
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
        	Matcher m= COPYRIGHT_PATTERN.matcher(src);
            if (m.find())
                return src; // already has the new copyright notice
            src= removeOldCopyright(src);
            return COPYRIGHT_NOTICE + src;
        }
    }

    private class LPGEPLCopyrightAdder implements ICopyrightAdder {
        private final Pattern COPYRIGHT_PATTERN = Pattern.compile(
            "%Notice\n" +
            "/.\n" +
            "////////////////////////////////////////////////////////////////////////////////\n" +
            "// Copyright (c) [0-9]+ IBM Corporation.\n" +
            "// All rights reserved. This program and the accompanying materials\n" +
            "// are made available under the terms of the Eclipse Public License v1.0\n" +
            "// which accompanies this distribution, and is available at\n" +
            "// http://www.eclipse.org/legal/epl-v10.html\n" +
            "//\n" +
            "//Contributors:\n" +
            "(//.*\n)+" + // 1 or more contributor lines
            "\n" +
            "////////////////////////////////////////////////////////////////////////////////\n" +
            "./\n" +
            "%End\n" +
            "\n");

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

        private final Pattern RULES_PATTERN= Pattern.compile("^\\s*%[Rr][Uu][Ll][Ee][Ss]");

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
        	Matcher cm= COPYRIGHT_PATTERN.matcher(src);
            if (cm.find())
                return src;
            src = removeOldCopyright(src);
            Matcher rm= RULES_PATTERN.matcher(src);
            int rulesLoc= -1;
            if (rm.find()) {
            	rulesLoc= rm.start(); // Look for the new keyword form ("%Rules") first; "XXX$Rules" is a legal right-hand side symbol reference
            }
            if (rulesLoc < 0) {
                rulesLoc= src.indexOf("$Rules");
            }
            if (rulesLoc < 0) {
            	rulesLoc= 0;
            }
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
    private final List<IFile> fChangedFiles= new ArrayList<IFile>();
    private CompositeChange fTopChange;

    private int fModCount= 0;

	public void addCopyrightTo(IResource resource) {
		computeChangeForCopyrights(resource);
        ReleaseTool.doPerformChange(fTopChange, "Adding copyright notices", fChangedFiles);
        ReleaseEngineeringPlugin.getMsgStream().println("Modified " + fModCount + " files.");
	}

	public CompositeChange computeChangeForCopyrights(IResource resource) {
		fChangedFiles.clear();
		fTopChange= new CompositeChange("Copyright additions");

        if (resource instanceof IFile) {
        	processFile((IFile) resource, fTopChange);
        } else if (resource instanceof IFolder) {
        	fSrcRoots.clear();
        	fSrcRoots.add((IFolder) resource);
        	traverseSrcRootsAddCopyrights(fTopChange);
        }
		return fTopChange;
	}

	public void addCopyrights() {
//      final IPreferenceStore prefStore= ReleaseEngineeringPlugin.getInstance().getPreferenceStore();

        fReleaseTool.collectMetaData(true);

		fChangedFiles.clear();
		fTopChange= new CompositeChange("Copyright additions");
        fSrcRoots.clear();

        List<FeatureInfo> featureInfos= fReleaseTool.getFeatureInfos();

        final Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		SelectFeatureInfosDialog sfid= new SelectFeatureInfosDialog(shell, featureInfos);

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

    	fModCount= 0;
        traverseSrcRootsAddCopyrights(fTopChange);

        if (fTopChange.getChildren().length == 0) {
        	MessageDialog.openInformation(shell, "No changes needed", "No source files lack a copyright notice.");
        	return;
        }

        Dialog d= new ConfirmChangedFilesDialog(shell, fTopChange);

        if (d.open() == Dialog.OK) {
            ReleaseTool.doPerformChange(fTopChange, "Adding copyright notices", fChangedFiles);
            ReleaseEngineeringPlugin.getMsgStream().println("Modified " + fModCount + " files.");
        }
    }

    private void traverseSrcRootsAddCopyrights(final CompositeChange topChange) {
    	final Map<IResource,CompositeChange> resource2Change= new HashMap<IResource, CompositeChange>();
		for(IFolder srcRoot: fSrcRoots) {
			IProject project= srcRoot.getProject();

			if (!resource2Change.containsKey(project)) {
				CompositeChange projChange= new CompositeChange("Project " + project.getName());
				topChange.add(projChange);
				resource2Change.put(project, projChange);
			}
	    	CompositeChange folderChange= new CompositeChange(/*"Adding notices to folder " + */srcRoot.getName());
			resource2Change.put(srcRoot, folderChange);
            try {
                srcRoot.accept(new IResourceVisitor() {
                    public boolean visit(IResource resource) throws CoreException {
                        if (resource.isDerived())
                            return false;

                        if (resource instanceof IFile) {
                            processFile((IFile) resource, resource2Change.get(resource.getParent()));
                        } else if (resource instanceof IFolder) {
                            ReleaseEngineeringPlugin.getMsgStream().println("  Scanning folder " + resource.getLocation().toPortableString());
                        	CompositeChange newComp= new CompositeChange(/*"Adding notices to folder " + */resource.getName());
                        	resource2Change.put(resource, newComp);
                        	resource2Change.get(resource.getParent()).add(newComp);
                        }
                        return true;
                    }
                });
            } catch (CoreException e) {
                logError(e);
                e.printStackTrace();
            }
        }
		pruneEmptyChanges(fTopChange);
	}

	private void pruneEmptyChanges(Change change) {
		if (change instanceof CompositeChange) {
			CompositeChange compositeChange= (CompositeChange) change;
			for(Change child: compositeChange.getChildren()) {
				pruneEmptyChanges(child);
			}
			if (compositeChange.getChildren().length == 0) {
				((CompositeChange) compositeChange.getParent()).remove(compositeChange);
			}
		}
	}

	private void processFile(IFile file, CompositeChange parentChange) {
		String fileExtension= file.getFileExtension();

		if (fExtensionMap.containsKey(fileExtension)) {
		    ICopyrightAdder copyrightAdder= fExtensionMap.get(fileExtension);

		    addCopyright(copyrightAdder, file, parentChange);
		}
	}

    private void addCopyright(ICopyrightAdder copyrightAdder, IFile file, CompositeChange parentChange) {
        try {
//          ReleaseEngineeringPlugin.getMsgStream().println("    Processing file " + file.getLocation().toPortableString());
            InputStream is= file.getContents();
            String origCont= getFileContents(new InputStreamReader(is));

            final String newCont= copyrightAdder.addCopyright(origCont);

            if (newCont.equals(origCont)) {
            	return;
            }

            ReleaseEngineeringPlugin.getMsgStream().println("        File " + file.getName() + " needs a copyright notice.");
            fModCount++;

            TextFileChange tfc= new TextFileChange("Add copyright to " + file.getName(), file);
	        long curFileLength= new File(file.getLocation().toOSString()).length();

	        tfc.setEdit(new MultiTextEdit());
	        tfc.addEdit(new ReplaceEdit(0, (int) curFileLength, newCont));

	        fChangedFiles.add(file);
	        parentChange.add(tfc);
        } catch(CoreException e) {
            logError(e);
            e.printStackTrace();
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
