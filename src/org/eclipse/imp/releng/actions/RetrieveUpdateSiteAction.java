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

import org.eclipse.imp.releng.dialogs.RetrieveUpdateSiteDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class RetrieveUpdateSiteAction implements IWorkbenchWindowActionDelegate {

    public void dispose() { }

    public void init(IWorkbenchWindow window) { }

    public void run(IAction action) {
    	new RetrieveUpdateSiteDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).open();
//        new WorkbenchReleaseTool().retrieveUpdateSiteProject();
    }

    public void selectionChanged(IAction action, ISelection selection) { }
}
