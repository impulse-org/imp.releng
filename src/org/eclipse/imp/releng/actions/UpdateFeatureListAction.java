package org.eclipse.imp.releng.actions;

import org.eclipse.imp.releng.WorkbenchReleaseTool;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class UpdateFeatureListAction implements IWorkbenchWindowActionDelegate {

    public void dispose() { }

    public void init(IWorkbenchWindow window) { }

    public void selectionChanged(IAction action, ISelection selection) { }

    public void run(IAction action) {
        new WorkbenchReleaseTool().updateFeatureList();
    }
}
