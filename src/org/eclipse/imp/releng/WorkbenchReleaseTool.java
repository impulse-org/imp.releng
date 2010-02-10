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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.imp.releng.dialogs.ConfirmChangedPluginsDialog;
import org.eclipse.imp.releng.dialogs.ConfirmDirtyFilesDialog;
import org.eclipse.imp.releng.dialogs.ConfirmProjectRetrievalDialog;
import org.eclipse.imp.releng.dialogs.ConfirmUpdateSitesDialog;
import org.eclipse.imp.releng.dialogs.UpdateSiteFeatureSetDialog;
import org.eclipse.imp.releng.dialogs.SelectFeatureInfosDialog;
import org.eclipse.imp.releng.dialogs.SelectFeatureProjectsDialog;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo;
import org.eclipse.imp.releng.metadata.PluginInfo.ChangeReason;
import org.eclipse.imp.releng.metadata.PluginInfo.ResourceChange;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo.FeatureRef;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

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

    protected Display getDisplay() {
        return PlatformUI.getWorkbench().getDisplay();
    }

    @Override
    protected void postError(String errorMsg, Exception e) {
        MessageDialog.openError(getShell(), "Error", errorMsg);
        ReleaseEngineeringPlugin.getMsgStream().println(errorMsg);
    }

    @Override
    protected void emitErrorMsg(String msg) {
        MessageConsoleStream msgStream= ReleaseEngineeringPlugin.getErrorStream();
        msgStream.println(msg);
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
        IProject proj= wsRoot.getProject(pi.fProjectName);
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

        Dialog d= new UpdateSiteFeatureSetDialog(getShell(), fUpdateSiteInfos, this);
        d.open();
    }

    @Override
    protected boolean confirmShareProject(IProject project) {
        return false;
    }

    @Override
    public boolean retrieveFeatures(boolean anonAccess) {
        collectMetaData(true);

        ConfirmUpdateSitesDialog d= new ConfirmUpdateSitesDialog(getShell(), fUpdateSiteInfos);

        if (d.open() != Dialog.OK) {
            return false;
        }

        List<UpdateSiteInfo> sites= d.getSites();
        return retrieveFeatures(sites, anonAccess);
    }

	public boolean retrieveFeatures(List<UpdateSiteInfo> sites, boolean anonAccess) {
		Map<String,Set<String>> featureProjectMap= new HashMap<String,Set<String>>();

        for(UpdateSiteInfo siteInfo: sites) {
            IProject updateProject= siteInfo.fProject;
            IFile featureSetFile= updateProject.getFile(anonAccess ? "featuresAnon.psf" : "features.psf");
            Map<String,Set<String>> providerToRefs= readProjectSet(featureSetFile);

            mergeMapInto(providerToRefs, featureProjectMap);
        }

        List<String> allProjects= collectProjectNamesFromProviderMap(featureProjectMap);
        ConfirmProjectRetrievalDialog cprd= new ConfirmProjectRetrievalDialog(getShell(), allProjects);

        if (cprd.open() == Dialog.OK) {
            retrieveProjectsWithProgress(featureProjectMap);
            return true;
        }
        return false;
	}

    @Override
    public boolean retrievePlugins(boolean anonAccess) {
        collectMetaData(true);

        if (fFeatureInfos.size() == 0)
            return false;

        SelectFeatureInfosDialog d= new SelectFeatureInfosDialog(getShell(), fFeatureInfos);

        if (d.open() != Dialog.OK)
            return false;

        List<FeatureInfo> featureInfos= d.getSelectedFeatures();

        return retrievePlugins(featureInfos, anonAccess);
    }

    public boolean retrievePlugins(List<FeatureInfo> featureInfos, boolean anonAccess) {
        final Map<String,Set<String>> pluginProjectRefs= new HashMap<String,Set<String>>();

        for(FeatureInfo featureInfo: featureInfos) {
            IProject featureProject= featureInfo.fProject;
            IFile projectSetFile= featureProject.getFile(anonAccess ? "pluginsAnon.psf" : "plugins.psf");
            Map<String,Set<String>> providerToRefs= readProjectSet(projectSetFile);

            mergeMapInto(providerToRefs, pluginProjectRefs);

            IFile extraProjectSetFile= featureProject.getFile(anonAccess ? "extraProjectsAnon.psf" : "extraProjects.psf");

            if (extraProjectSetFile.exists()) {
                Map<String,Set<String>> extraProviderToRefs= readProjectSet(extraProjectSetFile);

                mergeMapInto(extraProviderToRefs, pluginProjectRefs);
            }
        }

        List<String> allProjects= collectProjectNamesFromProviderMap(pluginProjectRefs);
        ConfirmProjectRetrievalDialog cprd= new ConfirmProjectRetrievalDialog(getShell(), allProjects);

        if (cprd.open() == Dialog.OK) {
            retrieveProjectsWithProgress(pluginProjectRefs);
            return true;
        }
        return false;
    }

    public void buildRelease() {
        collectMetaData(true);

        ConfirmUpdateSitesDialog d= new ConfirmUpdateSitesDialog(getShell(), fUpdateSiteInfos);

        if (d.open() != Dialog.OK) {
            return;
        }

        AntCorePreferences acp= AntCorePlugin.getPlugin().getPreferences();
        maybeCreateImpAntClasspathEntry(acp);
        maybeCreateImpForTaskEntry(acp);
        
        List<UpdateSiteInfo> sites= d.getSites();

        for(UpdateSiteInfo updateSiteInfo : sites) {
            IFile buildScript= updateSiteInfo.fProject.getFile("exportUpdate.xml");

            if (!buildScript.exists()) {
                MessageDialog.openError(getShell(), "Unable to find build script", "The build script exportUpdate.xml does not exist in project " + updateSiteInfo.fProject.getName());
            }
            // There doesn't appear to be enough published API to initiate an Ant build programmatically.
            AntLaunchShortcut als= new AntLaunchShortcut();
            als.setShowDialog(false);
            als.launch(buildScript.getFullPath(), buildScript.getProject(), ILaunchManager.RUN_MODE, "build.update.zip");
        }
    }

    private void maybeCreateImpAntClasspathEntry(AntCorePreferences acp) {
        IAntClasspathEntry[] addlCPEntries= acp.getAdditionalClasspathEntries();

        for(int i= 0; i < addlCPEntries.length; i++) {
            IAntClasspathEntry entry= addlCPEntries[i];
            if (entry.getEntryURL().getPath().contains("ant-imp.jar")) {
                return;
            }
        }
        if (!MessageDialog.openConfirm(getShell(), "Ant configuration missing classpath entry for 'for' task", "Add the necessary classpath entry?")) {
            return;
        }
        try {
            // There doesn't appear to be any published API to create an IAntClasspathEntry.
            IAntClasspathEntry[] newEntries= new IAntClasspathEntry[addlCPEntries.length + 1];
            Bundle relengBundle= Platform.getBundle(ReleaseEngineeringPlugin.kPluginID);
            URL jarLoc= FileLocator.toFileURL(FileLocator.find(relengBundle, new Path("ant-imp.jar"), null));

            System.arraycopy(addlCPEntries, 0, newEntries, 0, addlCPEntries.length);
            newEntries[addlCPEntries.length]= new AntClasspathEntry(jarLoc);
            acp.setAdditionalClasspathEntries(newEntries);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void maybeCreateImpForTaskEntry(AntCorePreferences acp) {
        List<Task> allTasks= acp.getTasks();

        for(Task task : allTasks) {
            if (task.getTaskName().equals("for")) {
                return;
            }
        }
        if (!MessageDialog.openConfirm(getShell(), "Ant configuration missing necessary task entry", "Add the necessary entry for the 'for' task?")) {
            return;
        }
        Task newTask= new Task();
        newTask.setClassName("org.eclipse.imp.ant.ForTask");
        newTask.setTaskName("for");
        acp.setCustomTasks(new Task[] { newTask });
    }

    private static class TemplateInfo {
        private final String fTemplateName;
        private final String fDestPath;

        public TemplateInfo(String templateName, String destPath) {
            fTemplateName= templateName;
            fDestPath= destPath;
        }
        public String getTemplateName() {
            return fTemplateName;
        }
        public String getDestPath() {
            return fDestPath;
        }
    }

    private final static List<TemplateInfo> sPluginTemplates= new ArrayList<TemplateInfo>();
    private final static List<TemplateInfo> sFeatureTemplates= new ArrayList<TemplateInfo>();
    private final static List<TemplateInfo> sUpdateTemplates= new ArrayList<TemplateInfo>();

    static {
        sPluginTemplates.add(new TemplateInfo("exportPlugin.xml", ""));
        sFeatureTemplates.add(new TemplateInfo("buildCommon.xml", ""));
        sFeatureTemplates.add(new TemplateInfo("exportFeature.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("buildCommon.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("buildFeatureCommon.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("buildPluginCommon.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("exportFeature.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("exportPlugin.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("exportUpdate.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("exportUpdateCustom.xml", ""));
        sUpdateTemplates.add(new TemplateInfo("SourceFeatureManifest.in", ""));
        sUpdateTemplates.add(new TemplateInfo("SourcePluginManifest.in", ""));
        sUpdateTemplates.add(new TemplateInfo("update.properties", ""));
    }

    public void addReleaseScripts() {
        collectMetaData(true);

        if (fFeatureInfos.size() == 0)
            return;

        Bundle relengBundle= ReleaseEngineeringPlugin.getInstance().getBundle();
        List<UpdateSiteInfo> updateSites= confirmUpdateSites();
        IProgressMonitor monitor= new NullProgressMonitor();

        for(UpdateSiteInfo updateSiteInfo : updateSites) {
            Map<String,String> updateSubs= new HashMap<String, String>();
            StringBuilder sb= new StringBuilder();

            for(FeatureRef featRef: updateSiteInfo.fFeatureRefs) {
                if (sb.length() > 0) { sb.append(","); }
                sb.append(featRef.getID());
            }
            String featureNames= sb.toString();

            updateSubs.put("%%UPDATE_PROJ_NAME%%", updateSiteInfo.fProject.getName());
            updateSubs.put("%%FEATURE_NAME_LIST%%", featureNames);

            for(TemplateInfo updateTemplate: sUpdateTemplates) {
                instantiateTemplate(updateTemplate.getTemplateName(), "updateTemplates/",
                        updateSiteInfo.fProject, updateTemplate.getDestPath(),
                        updateSubs, relengBundle, monitor);
            }
            // RMF 3 Feb 2010 - There is no longer a need for per-feature and per-plugin release scripts
//            for(FeatureRef featureRef: updateSiteInfo.fFeatureRefs) {
//                FeatureInfo featureInfo= findFeatureInfo(featureRef.getID());
//                Map<String,String> featureSubs= new HashMap<String, String>();
//
//                featureSubs.put("%%FEATURE_ID%%", featureRef.getID());
//                featureSubs.put("%%FEATURE_PROJ_NAME%%", featureInfo.fProject.getName());
//                featureSubs.putAll(updateSubs);
//
//                for(TemplateInfo featureTemplate: sFeatureTemplates) {
//                    instantiateTemplate(featureTemplate.getTemplateName(), "featureTemplates/",
//                            featureInfo.fProject, featureTemplate.getDestPath(),
//                            featureSubs, relengBundle, monitor);
//                }
//
//                for(PluginInfo pluginInfo: featureInfo.fPluginInfos) {
//                    IProject pluginProject= pluginInfo.fManifest.getProject();
//                    Map<String,String> pluginSubs= new HashMap<String, String>();
//
//                    pluginSubs.put("%%PLUGIN_ID%%", pluginInfo.fPluginID);
//                    pluginSubs.putAll(featureSubs);
//
//                    for(TemplateInfo pluginTemplate: sPluginTemplates) {
//                        instantiateTemplate(pluginTemplate.getTemplateName(), "pluginTemplates/",
//                                pluginProject, pluginTemplate.getDestPath(),
//                                pluginSubs, relengBundle, monitor);
//                    }
//                }
//            }
        }
    }

    private void instantiateTemplate(String templateName, String templatePath, IProject destProject, String destPath,
            Map<String,String> substitutions, Bundle relengBundle, IProgressMonitor monitor) {
        try {
            URL localURL= FileLocator.toFileURL(FileLocator.find(relengBundle, new Path(templatePath + templateName), null));
            String path= localURL.getPath();
            FileInputStream fis= new FileInputStream(path);
            DataInputStream is= new DataInputStream(fis);
            byte bytes[]= new byte[fis.available()];

            is.readFully(bytes);
            is.close();
            fis.close();

            String templateSrc= new String(bytes);
            String substSrc= performSubstitutions(templateSrc, substitutions);
            IFile destFile= destProject.getFile(new Path(destPath + templateName));

            if (destFile.exists()) {
                destFile.setContents(new ByteArrayInputStream(substSrc.getBytes()), true, true, monitor);
            } else {
                if (destPath.length() > 0) {
                    createSubFolders(destPath, destProject, monitor);
                }
                destFile.create(new ByteArrayInputStream(substSrc.getBytes()), true, monitor);
            }
        } catch (IOException e) {
            ReleaseEngineeringPlugin.getMsgStream().println(e.getMessage());
        } catch (CoreException e) {
            ReleaseEngineeringPlugin.getMsgStream().println(e.getMessage());
        }
    }

    public static String performSubstitutions(String contents, Map<String,String> replacements) {
        StringBuffer buffer= new StringBuffer(contents);
        
        for(Iterator<String> iter= replacements.keySet().iterator(); iter.hasNext();) {
            String key= iter.next();
            String value= replacements.get(key);
        
            if (value != null)
                replace(buffer, key, value);
        }
        return buffer.toString();
    }

    public static void replace(StringBuffer sb, String target, String substitute) {
        for(int index= sb.indexOf(target); index != -1; index= sb.indexOf(target))
            sb.replace(index, index + target.length(), substitute);
    }

    public static void createSubFolders(String folder, IProject project, IProgressMonitor monitor) throws CoreException {
        String[] subFolderNames= folder.split("[\\" + File.separator + "\\/]");
        String subFolderStr= "";

        for(int i= 0; i < subFolderNames.length; i++) {
            String childPath= subFolderStr + "/" + subFolderNames[i];
            Path subFolderPath= new Path(childPath);
            IFolder subFolder= project.getFolder(subFolderPath);

            if (!subFolder.exists())
                subFolder.create(true, true, monitor);
            subFolderStr= childPath;
        }
    }
}
