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

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.imp.releng.WorkbenchReleaseTool;
import org.eclipse.imp.releng.metadata.PluginInfo;
import org.eclipse.imp.releng.metadata.PluginInfo.ChangeReason;
import org.eclipse.imp.releng.metadata.PluginInfo.NewPluginChange;
import org.eclipse.imp.releng.metadata.PluginInfo.ResourceChange;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.ui.console.MessageConsoleStream;

public class DetailsDialog extends Dialog {
    private final PluginInfo fPi;

    private final int PATH_COL= 0;

    private final int FILE_COL= 1;

    private final int CHANGE_COL= 2;

    DetailsDialog(Shell shell, PluginInfo pi) {
        super(shell);
        fPi= pi;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area= (Composite) super.createDialogArea(parent);

        final Table table= new Table(area, SWT.NONE); // viewer.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        TableColumn pathCol= new TableColumn(table, SWT.BORDER);
        pathCol.setText("Path");
        pathCol.setResizable(true);

        TableColumn fileCol= new TableColumn(table, SWT.BORDER);
        fileCol.setText("File");
        fileCol.setResizable(true);

        TableColumn changeCol= new TableColumn(table, SWT.BORDER);
        changeCol.setText("Change");
        changeCol.setResizable(true);

        // TableColumn deltaCol= new TableColumn(table, SWT.BORDER);
        // deltaCol.setText("Deltas");
        // deltaCol.setResizable(true);

        TableViewer viewer= new TableViewer(table); // new TableViewer(area,
                                                    // SWT.SINGLE | SWT.V_SCROLL
                                                    // | SWT.BORDER);

        List<ChangeReason> changes= fPi.getAllChanges();
        for(ChangeReason change: changes) {
            if (change instanceof ResourceChange) {
                ResourceChange rc= (ResourceChange) change;
                TableItem ti= new TableItem(table, SWT.NONE);
                IPath path= rc.getPath();

                ti.setText(PATH_COL, path.removeLastSegments(1).toPortableString());
                ti.setText(FILE_COL, path.lastSegment());
                ti.setText(CHANGE_COL, rc.getType());
                // ti.setText(DELTA_COL, "deltas...");
                ti.setData(rc);
            } else if (change instanceof NewPluginChange) {
                TableItem ti= new TableItem(table, SWT.NONE);
                
                ti.setText(PATH_COL, "");
                ti.setText(FILE_COL, "");
                ti.setText(CHANGE_COL, "new plugin(?)");
                ti.setData(change);
            }
        }
        // For some reason, addMouseListener() seems to produce no callbacks for
        // double-click events???
        // So, use the lower-level addListener() API instead.
        table.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event event) {
                TableItem ti= table.getItem(new Point(event.x, event.y));
                if (ti != null) {
                    ResourceChange rc= (ResourceChange) ti.getData();
                    MessageConsoleStream mcs= ReleaseEngineeringPlugin.getMsgStream();
                    IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
                    IProject proj= wsRoot.getProject(fPi.fPluginID);
                    IFileHistoryProvider histProvider= RepositoryProvider.getProvider(proj).getFileHistoryProvider();

                    WorkbenchReleaseTool.dumpRecentRevisionsInfo(fPi, mcs, histProvider, rc, (IFile) wsRoot.findMember(rc.getPath()));
                    try {
                        mcs.flush();
                    } catch (IOException e) {
                    }
                }
            }
        });
        pathCol.pack();
        fileCol.pack();
        changeCol.pack();
        // deltaCol.pack();
        return area;
    }
}