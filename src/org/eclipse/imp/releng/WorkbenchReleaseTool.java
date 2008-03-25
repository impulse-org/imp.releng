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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.imp.releng.dialogs.ConfirmChangedPluginsDialog;
import org.eclipse.imp.releng.dialogs.ConfirmDirtyFilesDialog;
import org.eclipse.imp.releng.dialogs.ConfirmProjectRetrievalDialog;
import org.eclipse.imp.releng.dialogs.ConfirmUpdateSitesDialog;
import org.eclipse.imp.releng.dialogs.FeatureSetDialog;
import org.eclipse.imp.releng.dialogs.SelectFeatureInfosDialog;
import org.eclipse.imp.releng.dialogs.SelectFeatureProjectsDialog;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo;
import org.eclipse.imp.releng.metadata.PluginInfo.ChangeReason;
import org.eclipse.imp.releng.metadata.PluginInfo.ResourceChange;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;

public class WorkbenchReleaseTool extends ReleaseTool {
    public static void dumpRecentRevisionsInfo(PluginInfo pi, MessageConsoleStream mcs, IFileHistoryProvider histProvider, ResourceChange rc, IFile file) {
        List<IFileRevision> recentRevs= getRevisionsSinceLastRelease(file, pi, histProvider);

        mcs.println(rc.getPath().toPortableString() + ": " + rc.getType());
        for(IFileRevision rev: recentRevs) {
            mcs.println("  " + rev.getContentIdentifier() + ": " + rev.getAuthor() + ": " + rev.getComment());
        }
    }

    protected Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    @Override
    protected void postError(String errorMsg, Exception e) {
        MessageDialog.openError(getShell(), "Error", errorMsg);
        ReleaseEngineeringPlugin.getMsgStream().println(errorMsg);
    }

    @Override
    protected Set<IProject> selectFeatureProjects(final Set<IProject> allFeatureProjects) {
        final Set<IProject> selectedFeatureProjects= new HashSet<IProject>();

        Dialog featureDialog= new SelectFeatureProjectsDialog(getShell(), allFeatureProjects, selectedFeatureProjects);
        if (featureDialog.open() == Dialog.OK)
            return selectedFeatureProjects;
        return Collections.emptySet();
    }

    @Override
    protected List<FeatureInfo> selectFeatureInfos() {
        SelectFeatureInfosDialog featureDialog= new SelectFeatureInfosDialog(getShell(), fFeatureInfos);

        if (featureDialog.open() == Dialog.OK)
            return featureDialog.getSelectedFeatures();
        return Collections.emptyList();
    }

    // TODO Probably reconstitute this as a Forms-based "dashboard" page
    public void run() {
        Shell shell= getShell();
        Dialog d= new Dialog(shell) {
            @Override
            protected Control createDialogArea(Composite parent) {
                Composite area= (Composite) super.createDialogArea(parent);
                GridLayout grid= new GridLayout(3, true);
                area.setLayout(grid);
                Button saveFeatureProjectSetsButton= new Button(area, SWT.PUSH);
                saveFeatureProjectSetsButton.setText("Save Feature Project Sets...");
                Button checkOutButton= new Button(area, SWT.PUSH);
                checkOutButton.setText("Check out...");
                return area;
            }
        };
        d.open();
    }

    @Override
    protected boolean doConfirm(final Set<PluginInfo> changedPlugins, final Set<PluginInfo> unchangedPlugins) {
        Shell shell= getShell();
        if (changedPlugins.isEmpty()) {
            MessageDialog.openInformation(shell, "No plugins to release", "No plugins in the selected features have any changes to release!");
            return false;
        } else {
            Dialog confirmDialog = new ConfirmChangedPluginsDialog(shell, changedPlugins, unchangedPlugins);
            return (confirmDialog.open() == Dialog.OK);
        }
    }

    protected boolean confirmDirtyFiles(final Set<IFile> dirtyFiles) {
        if (dirtyFiles.isEmpty()) {
            return true;
        }
        Shell shell= getShell();
        Dialog d= new ConfirmDirtyFilesDialog(shell, dirtyFiles);
        return (d.open() == Dialog.OK);
    }

    private void showDetails(PluginInfo pi) {
        MessageConsoleStream mcs= ReleaseEngineeringPlugin.getMsgStream();
        List<ChangeReason> changes= pi.getAllChanges();
        IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
        IProject proj= wsRoot.getProject(pi.fPluginID);
        IFileHistoryProvider histProvider= RepositoryProvider.getProvider(proj).getFileHistoryProvider();

        for(ChangeReason change: changes) {
            if (change instanceof ResourceChange) {
                ResourceChange rc= (ResourceChange) change;
                IFile file= (IFile) wsRoot.findMember(rc.getPath());
                dumpRecentRevisionsInfo(pi, mcs, histProvider, rc, file);
            } else {
                mcs.println(change.toString());
            }
        }
        try {
            mcs.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<UpdateSiteInfo> confirmUpdateSites() {
        Shell shell= getShell();

        if (fUpdateSiteInfos.isEmpty()) {
            MessageDialog.openInformation(shell, "Nothing to update", "No update sites found!");
        } else {
            ConfirmUpdateSitesDialog confirmDialog= new ConfirmUpdateSitesDialog(shell, fUpdateSiteInfos);

            if (confirmDialog.open() == Dialog.OK) {
                return confirmDialog.getSites();
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    protected void confirmNoSiteUpdates() {
        Shell shell= getShell();

        MessageDialog.openWarning(shell, "Nothing to update", "No update sites need updating");
    }

    @Override
    public void updateFeatureList() {
        collectMetaData(true);

        if (fFeatureInfos.size() == 0)
            return;

        Dialog d= new FeatureSetDialog(getShell(), fUpdateSiteInfos, this);
        d.open();
    }

    @Override
    protected boolean confirmShareProject(IProject project) {
        return false;
    }

    @Override
    public void retrieveFeatures() {
        collectMetaData(true);

        ConfirmUpdateSitesDialog d= new ConfirmUpdateSitesDialog(getShell(), fUpdateSiteInfos);

        if (d.open() != Dialog.OK) {
            return;
        }

        List<UpdateSiteInfo> sites= d.getSites();
        Map<String,Set<String>> featureProjectMap= new HashMap<String,Set<String>>();

        for(UpdateSiteInfo siteInfo: sites) {
            IProject updateProject= siteInfo.fProject;
            IFile featureSetFile= updateProject.getFile("features.psf");
            Map<String,Set<String>> providerToRefs= readProjectSet(featureSetFile);

            mergeMapInto(providerToRefs, featureProjectMap);
        }

        List<String> allProjects= collectProjectNamesFromProviderMap(featureProjectMap);
        ConfirmProjectRetrievalDialog cprd= new ConfirmProjectRetrievalDialog(getShell(), allProjects);

        if (cprd.open() == Dialog.OK) {
            retrieveProjectsWithProgress(featureProjectMap);
        }
    }

    @Override
    public void retrievePlugins() {
        collectMetaData(true);

        if (fFeatureInfos.size() == 0)
            return;

        SelectFeatureInfosDialog d= new SelectFeatureInfosDialog(getShell(), fFeatureInfos);

        if (d.open() != Dialog.OK)
            return;

        List<FeatureInfo> featureInfos= d.getSelectedFeatures();

        final Map<String,Set<String>> pluginProjectRefs= new HashMap<String,Set<String>>();

        for(FeatureInfo featureInfo: featureInfos) {
            IProject featureProject= featureInfo.fProject;
            IFile projectSetFile= featureProject.getFile("feature.psf");
            Map<String,Set<String>> providerToRefs= readProjectSet(projectSetFile);

            mergeMapInto(providerToRefs, pluginProjectRefs);

            IFile extraProjectSetFile= featureProject.getFile("extraProjects.psf");

            if (extraProjectSetFile.exists()) {
                Map<String,Set<String>> extraProviderToRefs= readProjectSet(extraProjectSetFile);

                mergeMapInto(extraProviderToRefs, pluginProjectRefs);
            }
        }

        List<String> allProjects= collectProjectNamesFromProviderMap(pluginProjectRefs);
        ConfirmProjectRetrievalDialog cprd= new ConfirmProjectRetrievalDialog(getShell(), allProjects);

        if (cprd.open() == Dialog.OK) {
            retrieveProjectsWithProgress(pluginProjectRefs);
        }
    }
}
