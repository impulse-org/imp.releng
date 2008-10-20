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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.imp.releng.CopyrightAdder;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class AddThisCopyrightAction implements IWorkbenchWindowActionDelegate {
	private IResource fResource;

    public void dispose() { }

    public void init(IWorkbenchWindow window) { }

    public void run(IAction action) {
        new CopyrightAdder().addCopyrightTo(fResource);
    }

    public void selectionChanged(IAction action, ISelection selection) {
    	if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IResource) {
				fResource= (IResource) first;
			} else if (first instanceof IJavaElement) {
				fResource= ((IJavaElement) first).getResource();
			} else if (first instanceof IAdaptable) {
				fResource= (IResource) ((IAdaptable) first).getAdapter(IResource.class);
			}
    	} else if (selection instanceof ITextSelection) {
    		IEditorInput input= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput();
    		if (input instanceof IFileEditorInput) {
    			IFileEditorInput fileInp= (IFileEditorInput) input;
    			fResource= fileInp.getFile();
    		}
    	}
    }
}
