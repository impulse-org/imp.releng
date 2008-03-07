/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.imp.releng.metadata.UpdateSiteInfo;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Presents the given set of update sites as a set of checkboxes,
 * and permits the user to select the ones s/he wants to process.
 */
public class ConfirmUpdateSitesDialog extends Dialog {
    private final List<UpdateSiteInfo> fAllSiteInfos;

    private final List<UpdateSiteInfo> fSiteInfos= new ArrayList<UpdateSiteInfo>();

    public ConfirmUpdateSitesDialog(Shell shell, List<UpdateSiteInfo> allSiteInfos) {
        super(shell);
        fAllSiteInfos= allSiteInfos;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, "Proceed", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area= (Composite) super.createDialogArea(parent);
        // GridLayout grid= new GridLayout(2, true);
        // area.setLayout(grid);

        for(final UpdateSiteInfo site: fAllSiteInfos) {
            // Label siteID= new Label(area, SWT.NONE);
            Button siteButton= new Button(area, SWT.CHECK);

            siteButton.setText(site.fManifestFile.getProject().getName());
            siteButton.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
                    if (((Button) e.widget).getSelection()) {
                        fSiteInfos.add(site);
                    } else {
                        fSiteInfos.remove(site);
                    }
                }
            });
        }
        return area;
    }

    public List<UpdateSiteInfo> getSites() {
        return fSiteInfos;
    }
}
