/**
 * 
 */
package org.eclipse.imp.releng.dialogs;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class ConfirmDirtyFilesDialog extends Dialog {
    private final Set<IFile> fFiles;

    private final int PATH_COL= 0;

    private final int FILE_COL= 1;

    public ConfirmDirtyFilesDialog(Shell shell, Set<IFile> files) {
        super(shell);
        fFiles= files;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, "Proceed with dirty files", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

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
    
        TableViewer viewer= new TableViewer(table); // new TableViewer(area, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
    
        for(IFile file: fFiles) {
            TableItem ti= new TableItem(table, SWT.NONE);
            IPath path= file.getLocation();
    
            ti.setText(PATH_COL, path.removeLastSegments(1).toPortableString());
            ti.setText(FILE_COL, path.lastSegment());
        }
        pathCol.pack();
        fileCol.pack();
        return area;
    }
}