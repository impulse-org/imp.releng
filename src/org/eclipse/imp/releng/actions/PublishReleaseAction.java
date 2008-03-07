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
