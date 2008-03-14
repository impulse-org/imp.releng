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

package org.eclipse.imp.releng.prefs;

import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ReleaseEngineeringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public ReleaseEngineeringPreferencePage() {
        this("");
    }

    public ReleaseEngineeringPreferencePage(String title) {
        this(title, null);
    }

    public ReleaseEngineeringPreferencePage(String title, ImageDescriptor image) {
        super(title, image, GRID);
        setPreferenceStore(ReleaseEngineeringPlugin.getInstance().getPreferenceStore());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(ReleaseEngineeringPrefConstants.COUNT_BLANK_LINES, "Count blank lines", getFieldEditorParent()));

        addField(new BooleanFieldEditor(ReleaseEngineeringPrefConstants.SUPPRESS_COMMON_PREFIX, "Suppress common path prefix", getFieldEditorParent()));

        getPreferenceStore().addPropertyChangeListener(this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
}
