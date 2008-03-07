package org.eclipse.imp.releng.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.jface.preference.IPreferenceStore;


public class VersionIncrementerPreferenceInitializer extends AbstractPreferenceInitializer {

    public VersionIncrementerPreferenceInitializer() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefs = ReleaseEngineeringPlugin.getInstance().getPreferenceStore();

        prefs.setDefault(VersionIncrementerPrefConstants.COUNT_BLANK_LINES, false);
        prefs.setDefault(VersionIncrementerPrefConstants.SUPPRESS_COMMON_PREFIX, true);
    }
}
