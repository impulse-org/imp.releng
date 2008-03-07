package org.eclipse.imp.releng.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class TagFeaturesAction implements IWorkbenchWindowActionDelegate {

    public void dispose() { }

    public void init(IWorkbenchWindow window) { }

    public void selectionChanged(IAction action, ISelection selection) { }

    public void run(IAction action) {
        // For inspiration, see org.eclipse.releng.tools.TagAndReleaseOperation
        //
        // N.B. For SVN, which doesn't support tags, this may have to copy the
        // given folder to a name that embeds the desired tag, and set the SVN
        // "final" property on the resulting folder.
        //
        String featureID= "org.eclipse.imp.runtime";
        String featureVersion= "3.2.72";
        CVSTag tag= new CVSTag(featureID + "-" + featureVersion, CVSTag.VERSION);
        TagOperation tagOp= null; // new TagOperation();
    }
}
