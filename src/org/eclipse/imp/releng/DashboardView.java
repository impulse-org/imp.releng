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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo.FeatureRef;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.wizards.NewWizardRegistry;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.wizards.IWizardDescriptor;

public class DashboardView extends ViewPart {
    private FormToolkit fToolkit;

    private ScrolledForm fForm;

    private ReleaseTool fReleaseTool= new WorkbenchReleaseTool();

    public DashboardView() {
        fReleaseTool.collectMetaData(true);
    }

    public void setReleaseTool(ReleaseTool tool) {
        fReleaseTool= tool;
    }

    @Override
    public void createPartControl(Composite parent) {
        fToolkit= new FormToolkit(parent.getDisplay());
        fToolkit.setBorderStyle(SWT.BORDER);

        fForm= fToolkit.createScrolledForm(parent);
        fForm.setText("IMP Release Engineering");

        TableWrapLayout layout= new TableWrapLayout();

        layout.numColumns= 2;
        fForm.getBody().setLayout(layout);

        createUpdateSiteSection();

        createFeatureListSection();

        createPluginListSection();

        Button saveButton= fToolkit.createButton(fForm.getBody(), "Save", SWT.PUSH);

        saveButton.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.FILL, 1, 1));
        saveButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                System.out.println("hey!");
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });

        Button revertButton= fToolkit.createButton(fForm.getBody(), "Revert", SWT.PUSH);

        revertButton.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.FILL, 1, 1));
        revertButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                System.out.println("wha?");
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
    }

    private UpdateSiteInfo fSelectedSite;

    private FeatureInfo fSelectedFeature;

    private Table fFeatureTable;

    private Table fPluginTable;

    private void populatePluginsTable() {
        fPluginTable.removeAll();
        for(PluginInfo pluginInfo: fSelectedFeature.fPluginInfos) {
            TableItem ti= new TableItem(fPluginTable, SWT.NONE);
            ti.setText(0, pluginInfo.fPluginID);
        }
        for(int i=0; i < fPluginTable.getColumnCount(); i++) {
            fPluginTable.getColumn(i).pack();
        }
        fPluginTable.pack();
    }

    private void createPluginListSection() {
        Section pluginsSection= fToolkit.createSection(fForm.getBody(), Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.CLIENT_INDENT | Section.EXPANDED);

        pluginsSection.setText("Feature Plugins");
        pluginsSection.setDescription("The set of plugins included in the selected feature");
        pluginsSection.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.FILL, 1, 2));

        Composite pluginsClient= fToolkit.createComposite(pluginsSection);
        TableWrapLayout pluginsLayout= new TableWrapLayout();

        pluginsLayout.numColumns= 2;
        pluginsLayout.verticalSpacing= 3;
        pluginsClient.setLayout(pluginsLayout);
        pluginsSection.setClient(pluginsClient);

        fPluginTable= fToolkit.createTable(pluginsClient, SWT.NONE);
        TableColumn pluginIDCol= new TableColumn(fPluginTable, SWT.LEFT);
        pluginIDCol.setText("Plugin ID");
        TableViewer pluginTableViewer= new TableViewer(fPluginTable);
        populatePluginsTable();
        fPluginTable.setHeaderVisible(true);
        fPluginTable.setLinesVisible(true);

        Composite buttonComposite= fToolkit.createComposite(pluginsClient);
        buttonComposite.setLayout(new RowLayout(SWT.VERTICAL));

        Button newPluginButton= fToolkit.createButton(buttonComposite, "New Plugin...", SWT.PUSH);
        addWizardRunner(newPluginButton, "org.eclipse.pde.ui.NewProjectWizard");

        Button addPluginButton= fToolkit.createButton(buttonComposite, "Add Plugin...", SWT.PUSH);
        addPluginButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                // TODO Need FeatureInfo to be able to rewrite its manifest
                // from its list of PluginInfos, rather than its XML Document.
//              PluginInfo pluginInfo= fReleaseTool.selectPlugin();
//
//              fSelectedFeature.addPlugin(pluginInfo);
//              populatePluginsTable();
//              fReleaseTool.rewriteFeatureManifest(fSelectedFeature);
            }
        });
    }

    private void createFeatureListSection() {
        Section featuresSection= fToolkit.createSection(fForm.getBody(), Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.CLIENT_INDENT | Section.EXPANDED);

        featuresSection.setText("Update Site Features");
        featuresSection.setDescription("The set of features included in the selected update site");
        featuresSection.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.FILL, 1, 2));

        Composite featuresClient= fToolkit.createComposite(featuresSection);
        TableWrapLayout featuresLayout= new TableWrapLayout();

        featuresLayout.numColumns= 2;
        featuresLayout.verticalSpacing= 3;
        featuresClient.setLayout(featuresLayout);
        featuresSection.setClient(featuresClient);

        fFeatureTable= fToolkit.createTable(featuresClient, SWT.NONE);
        TableColumn featureIDCol= new TableColumn(fFeatureTable, SWT.LEFT);
        featureIDCol.setText("Feature ID");
        TableViewer featureTableViewer= new TableViewer(fFeatureTable);
        populateFeaturesTable();
        fFeatureTable.setHeaderVisible(true);
        fFeatureTable.setLinesVisible(true);
        fFeatureTable.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fSelectedFeature= (FeatureInfo) e.item.getData();
                populatePluginsTable();
            }
        });

        Composite buttonComposite= fToolkit.createComposite(featuresClient);
        buttonComposite.setLayout(new RowLayout(SWT.VERTICAL));

        Button newFeatureButton= fToolkit.createButton(buttonComposite, "New Feature...", SWT.PUSH);
        addWizardRunner(newFeatureButton, "org.eclipse.pde.ui.NewFeatureProjectWizard");

        Button incrementVersionsButton= fToolkit.createButton(buttonComposite, "Increment Feature Versions...", SWT.PUSH);
        incrementVersionsButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.incrementVersions();
            }
        });

        Button addFeatureButton= fToolkit.createButton(buttonComposite, "Add Feature...", SWT.PUSH);
        addFeatureButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                List<FeatureInfo> featureInfos= fReleaseTool.selectFeatureInfos();
                for(FeatureInfo info: featureInfos) {
                    fSelectedSite.fFeatureRefs.add(new FeatureRef(info.fFeatureID, info.fFeatureVersion, info.getURL(), findCategoryFor(info, fSelectedSite)));
                }
                populateFeaturesTable();
                fReleaseTool.rewriteUpdateSiteManifests(Collections.singletonList(fSelectedSite));
            }
        });

        Button updateFeatureProjectSetsButton= fToolkit.createButton(buttonComposite, "Update the Feature Project Set...", SWT.PUSH);
        updateFeatureProjectSetsButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.updateFeatureProjectSets();
            }
        });

        Button updateAllFeatureProjectSetsButton= fToolkit.createButton(buttonComposite, "Update All Feature Project Sets...", SWT.PUSH);
        updateAllFeatureProjectSetsButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.updateFeatureProjectSets();
            }
        });

        Button tagFeaturesButton= fToolkit.createButton(buttonComposite, "Tag Feature Versions...", SWT.PUSH);
        tagFeaturesButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.tagFeatures();
            }
        });
        buttonComposite.pack();
    }

    protected String findCategoryFor(FeatureInfo info, UpdateSiteInfo siteInfo) {
        for(FeatureRef featureRef: siteInfo.fFeatureRefs) {
            if (featureRef.getID().equals(info.fFeatureID)) {
                return featureRef.getCategory();
            }
        }
        return siteInfo.fCategories.get(0).getName();
    }

    private void populateFeaturesTable() {
        fFeatureTable.removeAll();
        Set<FeatureInfo> features= new HashSet<FeatureInfo>();
        for(FeatureRef featureRef: fSelectedSite.fFeatureRefs) {
            features.add(fReleaseTool.findFeatureInfo(featureRef.getID()));
        }
        for(FeatureInfo featureInfo: features) {
            TableItem ti= new TableItem(fFeatureTable, SWT.NONE);
            ti.setText(0, featureInfo.fFeatureID);
            ti.setData(featureInfo);
        }
        fSelectedFeature= features.iterator().next();
        for(int i=0; i < fFeatureTable.getColumnCount(); i++) {
            fFeatureTable.getColumn(i).pack();
        }
        fFeatureTable.pack();
    }

    private void createUpdateSiteSection() {
        Section siteSection= fToolkit.createSection(fForm.getBody(), Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.CLIENT_INDENT | Section.EXPANDED);

        siteSection.setText("Update Sites");
        siteSection.setDescription("Set of workspace update site projects");
        siteSection.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.FILL, 1, 2));

        Composite siteSectionClient= fToolkit.createComposite(siteSection);
        TableWrapLayout siteSectLayout= new TableWrapLayout();

        siteSectionClient.setLayout(siteSectLayout);

        siteSectLayout.numColumns= 2;
        siteSectLayout.verticalSpacing= 3;
        siteSectionClient.setLayout(siteSectLayout);
        siteSection.setClient(siteSectionClient);

        Table siteTable= fToolkit.createTable(siteSectionClient, SWT.NONE);

        TableColumn siteNameCol= new TableColumn(siteTable, SWT.LEFT);
        siteNameCol.setText("Site name");
        TableViewer siteTableViewer= new TableViewer(siteTable);
        for(UpdateSiteInfo siteInfo: fReleaseTool.fUpdateSiteInfos) {
            TableItem ti= new TableItem(siteTable, SWT.NONE);
            ti.setText(0, siteInfo.fProject.getName());
            ti.setData(siteInfo);
        }
        fSelectedSite= fReleaseTool.fUpdateSiteInfos.get(0);
        siteTable.setHeaderVisible(true);
        siteTable.setLinesVisible(true);
        siteTable.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fSelectedSite= (UpdateSiteInfo) e.item.getData();
                populateFeaturesTable();
            }
        });
        siteNameCol.pack();
        siteTable.pack();

        Composite buttonComposite= fToolkit.createComposite(siteSectionClient);
        buttonComposite.setLayout(new RowLayout(SWT.VERTICAL));

        Button newSiteButton= fToolkit.createButton(buttonComposite, "New Site...", SWT.PUSH);
        addWizardRunner(newSiteButton, "org.eclipse.pde.ui.NewSiteProjectWizard");

        Button updateSiteContentsButton= fToolkit.createButton(buttonComposite, "Update the Site Manifest...", SWT.PUSH);
        updateSiteContentsButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.updateUpdateSites();
            }
        });

        Button updateUpdateSiteProjectSetButton= fToolkit.createButton(buttonComposite, "Update the Site Project Set...", SWT.PUSH);
        updateUpdateSiteProjectSetButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.writeSiteFeatureSet(fSelectedSite);
            }
        });
        Button updateAllUpdateSiteProjectSetsButton= fToolkit.createButton(buttonComposite, "Update All Site Project Sets...", SWT.PUSH);
        updateAllUpdateSiteProjectSetsButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                fReleaseTool.writeAllSiteFeatureSets();
            }
        });
    }

    private void addWizardRunner(Button button, final String wizardID) {
        button.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                try {
                    runNewWizard(wizardID);
                } catch(CoreException exc) {
                }
            }
        });
    }

    @Override
    public void setFocus() {
        fForm.setFocus();
    }

    @Override
    public void dispose() {
        fToolkit.dispose();
        super.dispose();
    }

    private void runNewWizard(String wizardID) throws CoreException {
        IWorkbenchWindow win= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        final String newSiteWizardID= wizardID;
        NewWizardRegistry reg= NewWizardRegistry.getInstance();
        IWizardDescriptor wd= reg.findWizard(newSiteWizardID);
        IWorkbenchWizard wizard= wd.createWizard();
        WizardDialog wizDialog= new WizardDialog(win.getShell(), wizard);
        wizDialog.open();
    }
}
