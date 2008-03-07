/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.imp.releng.ReleaseTool;
import org.eclipse.imp.releng.WorkbenchReleaseTool;
import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.imp.releng.metadata.UpdateSiteInfo;
import org.eclipse.imp.releng.utils.Pair;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Presents a dialog that permits the user to select an update site,
 * edit the set of published features, and then update the feature
 * project set for that update site.
 */
public class FeatureSetDialog extends Dialog {
    static class FeatureTableItem {
        String fFeatureId;

        public FeatureTableItem(String featureID) {
            fFeatureId= featureID;
        }

        public String getName() {
            return fFeatureId;
        }

        public void setName(String name) {
            this.fFeatureId= name;
        }
    }

    private final List<UpdateSiteInfo> fSites;

    private static final int FEATURE_ID_COLUMN = 0;

    private static final String FEATURE_ID_PROPERTY= "featureID";

    private List<String> fValidFeatures= new ArrayList<String>();

    private UpdateSiteInfo fSite;

    private List<FeatureTableItem> fItems= new ArrayList<FeatureTableItem>();

    private Combo fSiteCombo;

    private TableViewer fTableViewer;

    private final ReleaseTool fReleaseTool;

    public FeatureSetDialog(Shell shell, List<UpdateSiteInfo> sites, ReleaseTool releaseTool) {
        super(shell);
        fSites= sites;
        fReleaseTool= releaseTool;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        Button proceedButton= createButton(parent, IDialogConstants.OK_ID, "Proceed", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    
        proceedButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                List<FeatureInfo> features= new ArrayList<FeatureInfo>();
    
                for(int i= 0; i < fItems.size(); i++) {
                    FeatureTableItem item= fItems.get(i);
    
                    features.add(fReleaseTool.findFeatureInfo(item.fFeatureId));
                }
                writeSiteFeatureSet(features, fSite.fProject);
            }
        });
    }

    private void createCellEditor(final TableViewer tableViewer) {
        final Table table= tableViewer.getTable();
    
        tableViewer.setCellModifier(new ICellModifier() {
            public boolean canModify(Object element, String property) {
                return true;
            }
            public Object getValue(Object element, String property) {
                if (FEATURE_ID_PROPERTY.equals(property)) {
                    return fValidFeatures.indexOf(((FeatureTableItem) element).fFeatureId);
                }
                return -1;
            }
            public void modify(Object element, String property, Object value) {
                TableItem tableItem = (TableItem) element;
                FeatureTableItem data = (FeatureTableItem) tableItem.getData();
                if (FEATURE_ID_PROPERTY.equals(property)) {
                    int idx= ((Integer) value).intValue();
                    if (idx >= 0) {
                        data.setName(fValidFeatures.get(idx));
                    }
                }
//              setDirty(true);
                tableViewer.refresh(data);
            }
        });

        ComboBoxCellEditor featureEditor = new ComboBoxCellEditor(table, fValidFeatures.toArray(new String[fValidFeatures.size()]));

        tableViewer.setCellEditors(new CellEditor[] { featureEditor });
        tableViewer.setColumnProperties(new String[] { FEATURE_ID_PROPERTY });
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 350);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite area= (Composite) super.createDialogArea(parent);

        fSiteCombo= new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
        for(UpdateSiteInfo site: fSites) {
            fSiteCombo.add(site.fProject.getName());
        }

        Composite featureListArea= new Composite(area, SWT.NONE);
        RowLayout featureListLayout= new RowLayout(SWT.HORIZONTAL);
        featureListLayout.fill= true;
        featureListLayout.pack= true;
        featureListArea.setLayout(featureListLayout);

        Composite featureTableArea= new Composite(featureListArea, SWT.NONE);
        RowLayout featureTableLayout= new RowLayout(SWT.VERTICAL);
        featureTableArea.setLayout(featureTableLayout);
        Label featureLabel= new Label(featureTableArea, SWT.NONE);
        featureLabel.setText("Features:");
        final Table table= new Table(featureTableArea, SWT.NONE); // viewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        final TableColumn featureCol= new TableColumn(table, SWT.BORDER);
        featureCol.setText("Feature");
        featureCol.setResizable(true);

        for(FeatureInfo fi: fReleaseTool.getFeatureInfos()) {
            fValidFeatures.add(fi.fFeatureID);
        }

        fTableViewer= new TableViewer(table);
        createCellEditor(fTableViewer);
        createLabelProvider();

        fSiteCombo.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                int idx= ((Combo) e.widget).getSelectionIndex();
                fSite= fSites.get(idx);
                table.removeAll();
                List<FeatureInfo> features= fReleaseTool.readUpdateFeatureInfos(fSite);
                for(FeatureInfo feature: features) {
                    FeatureTableItem item= new FeatureTableItem(feature.fFeatureID);
                    fItems.add(item);
                    fTableViewer.add(item);
                }
                featureCol.pack();
                table.pack();
                area.pack();
                getShell().pack();
            }
        });

        createButtonArea(featureListArea, table);
        return area;
    }

    private void createButtonArea(final Composite featureListArea, final Table table) {
        Composite opButtonArea= new Composite(featureListArea, SWT.NONE);
        RowLayout opButtonLayout= new RowLayout(SWT.VERTICAL);
        opButtonArea.setLayout(opButtonLayout);

        Button addFeatureButton= new Button(opButtonArea, SWT.PUSH);
        addFeatureButton.setText("Add");
        addFeatureButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                FeatureTableItem item= new FeatureTableItem("");
                // TODO Initialize with first available feature ID?
                fItems.add(item);
                fTableViewer.add(item);
//              // Now remove the features already appearing elsewhere in the list
//              TableItem[] curFeatures= table.getItems();
//              for(int i= 0; i < curFeatures.length; i++) {
//                  TableItem feature= curFeatures[i];
//                  featureIDs.remove(((EditableTableItem) feature.getData()).fFeatureId);
//              }
                featureListArea.pack();
                table.pack();
                getShell().pack();
            }
        });
        Button removeFeatureButton= new Button(opButtonArea, SWT.PUSH);
        removeFeatureButton.setText("Remove");
        removeFeatureButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                for(int i= 0; i < fItems.size(); i++) {
                    fTableViewer.remove(fItems.get(i));
                }
            }
        });
    }

    private void createLabelProvider() {
        fTableViewer.setLabelProvider(new ITableLabelProvider() {
            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }
            public String getColumnText(Object element, int columnIndex) {
                switch (columnIndex) {
                case FEATURE_ID_COLUMN:
                    return ((FeatureTableItem) element).getName();
                }
                return "";
            }
            public void addListener(ILabelProviderListener listener) { }
            public void dispose() { }
            public boolean isLabelProperty(Object element, String property) {
                return false;
            }
            public void removeListener(ILabelProviderListener listener) { }
        });
    }

    @Override
    protected int getShellStyle() {
        return super.getShellStyle() | SWT.RESIZE;
    }

    private void writeSiteFeatureSet(List<FeatureInfo> features, IProject hostProject) {
        Map<String/*repoTypeID*/,Set<String/*repoRef*/>> repoRefMap= new HashMap<String,Set<String>>();

        for(FeatureInfo feature: features) {
            Pair<String/*repoTypeID*/,String/*repoRef*/> repoDesc= fReleaseTool.getRepoRefForProject(feature.fProject);

            WorkbenchReleaseTool.addMapEntry(repoDesc.first, repoDesc.second, repoRefMap);
        }
        fReleaseTool.writeProjectSet(repoRefMap, hostProject, "features.psf");
    }
}
