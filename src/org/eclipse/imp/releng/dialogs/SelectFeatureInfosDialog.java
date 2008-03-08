/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.imp.releng.metadata.FeatureInfo;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SelectFeatureInfosDialog extends Dialog {
    private final List<FeatureInfo> fAllFeatureInfos= new ArrayList<FeatureInfo>();
    private final List<FeatureInfo> fFeatures= new ArrayList<FeatureInfo>();

    /**
     * @param shell the parent shell
     * @param features on entry, the Set of all known features; on return,
     * the set of user-selected features
     */
    public SelectFeatureInfosDialog(Shell shell, Collection<FeatureInfo> features) {
        super(shell);
        fAllFeatureInfos.addAll(features);
        Collections.sort(fAllFeatureInfos,
                new Comparator<FeatureInfo>() {
                    public int compare(FeatureInfo f1, FeatureInfo f2) {
                        return f1.fFeatureID.compareTo(f2.fFeatureID);
                    }
                });
    }

    public List<FeatureInfo> getSelectedFeatures() {
        return fFeatures;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area= (Composite) super.createDialogArea(parent);
        Label header= new Label(area, SWT.NULL);
        header.setText("Select features to process:");
        for(final FeatureInfo fi: fAllFeatureInfos) {
            Button featureCheckBox= new Button(area, SWT.CHECK);
    
            featureCheckBox.setText(fi.fFeatureID);
            featureCheckBox.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) { }
                public void widgetSelected(SelectionEvent e) {
                    if (((Button) e.widget).getSelection())
                        fFeatures.add(fi);
                    else
                        fFeatures.remove(fi);
                }
            });
        }
        return area;
    }
}