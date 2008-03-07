package org.eclipse.imp.releng.prefs;

import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class VersionIncrementerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public VersionIncrementerPreferencePage() {
        this("");
    }

    public VersionIncrementerPreferencePage(String title) {
        this(title, null);
    }

    public VersionIncrementerPreferencePage(String title, ImageDescriptor image) {
        super(title, image, GRID);
        setPreferenceStore(ReleaseEngineeringPlugin.getInstance().getPreferenceStore());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(VersionIncrementerPrefConstants.COUNT_BLANK_LINES, "Count blank lines", getFieldEditorParent()));

        addField(new BooleanFieldEditor(VersionIncrementerPrefConstants.SUPPRESS_COMMON_PREFIX, "Suppress common path prefix", getFieldEditorParent()));

        getPreferenceStore().addPropertyChangeListener(this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
}
