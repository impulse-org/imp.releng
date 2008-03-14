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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SelectFeatureProjectsDialog extends Dialog {
    private final Set<IProject> fProjects;

    private final Set<IProject> fProjects2;

    public SelectFeatureProjectsDialog(Shell shell, Set<IProject> projects, Set<IProject> projects2) {
        super(shell);
        fProjects= projects;
        fProjects2= projects2;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area= (Composite) super.createDialogArea(parent);
        Label header= new Label(area, SWT.NULL);
        header.setText("Select features to process:");
        for(final IProject proj: fProjects) {
            Button featureCheckBox= new Button(area, SWT.CHECK);
    
            featureCheckBox.setText(proj.getName());
            featureCheckBox.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) { }
                public void widgetSelected(SelectionEvent e) {
                    if (((Button) e.widget).getSelection())
                        fProjects2.add(proj);
                    else
                        fProjects2.remove(proj);
                }
            });
        }
        return area;
    }
}