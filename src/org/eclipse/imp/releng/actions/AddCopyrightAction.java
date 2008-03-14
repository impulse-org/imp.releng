package org.eclipse.imp.releng.actions;

import org.eclipse.imp.releng.CopyrightAdder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class AddCopyrightAction implements IWorkbenchWindowActionDelegate {
    public void dispose() { }

    public void init(IWorkbenchWindow window) { }

    public void run(IAction action) {
        new CopyrightAdder().addCopyrights();
    }

    public void selectionChanged(IAction action, ISelection selection) { }
}
