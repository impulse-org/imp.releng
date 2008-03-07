/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.HashSet;
import java.util.Set;

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
    private final Set<FeatureInfo> fAllFeatureInfos= new HashSet<FeatureInfo>();
    private final Set<FeatureInfo> fFeatures= new HashSet<FeatureInfo>();

    /**
     * @param shell the parent shell
     * @param features on entry, the Set of all known features; on return,
     * the set of user-selected features
     */
    public SelectFeatureInfosDialog(Shell shell, Set<FeatureInfo> features) {
        super(shell);
        fAllFeatureInfos.addAll(features);
        fFeatures.addAll(features);
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