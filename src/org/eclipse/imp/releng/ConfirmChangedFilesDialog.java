/*******************************************************************************
* Copyright (c) 2008 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*******************************************************************************/

package org.eclipse.imp.releng;

import org.eclipse.imp.releng.dialogs.ConfirmChangedFileViewer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

final class ConfirmChangedFilesDialog extends Dialog {
	private final CompositeChange fTopChange;

	ConfirmChangedFilesDialog(Shell parentShell, CompositeChange topChange) {
		super(parentShell);
		fTopChange= topChange;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Add copyright changes");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(750, 350);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea= (Composite) super.createDialogArea(parent);
		Composite composite= new Composite(dialogArea, SWT.NONE);
	    GridData gd= new GridData(GridData.FILL, GridData.FILL, true, true);
	    gd.heightHint= 270;
	    gd.widthHint= 720;
	    composite.setLayoutData(gd);
	    GridLayout layout= new GridLayout();
	    composite.setLayout(layout);

	    final Tree chgTreeControl= createChangeTreeViewer(composite);

	    gd= new GridData(GridData.FILL_BOTH);
	    gd.heightHint= 120;
	    gd.widthHint= 720;
	    chgTreeControl.setLayoutData(gd);

	    final ConfirmChangedFileViewer v= new ConfirmChangedFileViewer();
	    v.createControl(composite);
	    gd= new GridData(GridData.FILL_BOTH);
	    gd.heightHint= 150;
	    gd.widthHint= 720;
	    v.getControl().setLayoutData(gd);

	    setupSelectionListener(chgTreeControl, v);
	    return dialogArea;
	}

	private void setupSelectionListener(final Tree chgTree, final ConfirmChangedFileViewer v) {
		chgTree.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) { }
			public void widgetSelected(SelectionEvent e) {
				TextFileChange tfc= (TextFileChange) e.item.getData();

				v.setInput(ConfirmChangedFileViewer.createInput(tfc));
			}
	    });
	}

	private Tree createChangeTreeViewer(Composite parent) {
		Tree tree= new Tree(parent, SWT.SINGLE);
		populateSubTree(tree, fTopChange);
		return tree;
	}

	private void populateSubTree(Tree parent, Change change) {
		if (change instanceof CompositeChange) {
			CompositeChange compChange= (CompositeChange) change;

			for(Change child: compChange.getChildren()) {
				TreeItem newItem= new TreeItem(parent, SWT.NONE);
				newItem.setText(child.getName());
				newItem.setData(child);
				populateSubTree(newItem, child);
			}
		}
	}

	private void populateSubTree(TreeItem parentItem, Change change) {
		if (change instanceof CompositeChange) {
			CompositeChange compChange= (CompositeChange) change;

			for(Change child: compChange.getChildren()) {
				TreeItem newItem= new TreeItem(parentItem, SWT.NONE);
				newItem.setText(child.getName());
				newItem.setData(child);
				populateSubTree(newItem, child);
			}
		}
	}
}