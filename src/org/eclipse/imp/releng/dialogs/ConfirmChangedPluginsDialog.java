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

/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.Set;

import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ConfirmChangedPluginsDialog extends Dialog {
    static class DetailsButtonListener implements SelectionListener {
        private final PluginInfo fPi;

        private DetailsButtonListener(PluginInfo pi) {
            fPi= pi;
        }

        public void widgetDefaultSelected(SelectionEvent e) {}

        public void widgetSelected(SelectionEvent e) {
            Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            Dialog d= new DetailsDialog(shell, fPi);
            d.open();
        }
    }

    private final Set<PluginInfo> fChangedPlugins;

    private final Set<PluginInfo> fUnchangedPlugins2;

    public ConfirmChangedPluginsDialog(Shell shell, Set<PluginInfo> changedPlugins, Set<PluginInfo> unchangedPlugins) {
        super(shell);
        fChangedPlugins= changedPlugins;
        fUnchangedPlugins2= unchangedPlugins;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Plugin states for selected features");
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
        GridLayout grid= new GridLayout(3, true);
        area.setLayout(grid);
        for(final PluginInfo pi : fChangedPlugins) {
            Label pluginID= new Label(area, SWT.NONE);
            Label pluginVersion= new Label(area, SWT.NONE);

            pluginID.setText(pi.fPluginID);
            pluginVersion.setText(pi.fPluginNewVersion);

            if (pi.getChangeState().isChange()) {
                Button detailsButton= new Button(area, SWT.PUSH);
                detailsButton.setText("details...");
                detailsButton.addSelectionListener(new DetailsButtonListener(pi));
            } else {
                Label changeReason= new Label(area, SWT.NONE);
                changeReason.setText(pi.getChangeState().toString());
            }
        }
        for(PluginInfo pi : fUnchangedPlugins2) {
            Label pluginID= new Label(area, SWT.NONE);
            Label pluginVersion= new Label(area, SWT.NONE);
            Label changeReason= new Label(area, SWT.NONE);

            pluginID.setText(pi.fPluginID);
            pluginVersion.setText("");
            changeReason.setText("<unchanged>");
        }
        return area;
    }
}