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

package org.eclipse.imp.releng.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class PublishReleaseAction implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow fWorkbenchWindow;

    public void dispose() { }

    public void init(IWorkbenchWindow window) {
        fWorkbenchWindow= window;
    }

    public void run(IAction action) {
        MessageDialog.openInformation(fWorkbenchWindow.getShell(), "Unimplemented", "Publish Release has not been implemented.");
    }

    public void selectionChanged(IAction action, ISelection selection) { }
}
