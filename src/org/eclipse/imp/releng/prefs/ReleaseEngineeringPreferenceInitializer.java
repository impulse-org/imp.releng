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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;
import org.eclipse.jface.preference.IPreferenceStore;


public class ReleaseEngineeringPreferenceInitializer extends AbstractPreferenceInitializer {

    public ReleaseEngineeringPreferenceInitializer() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefs = ReleaseEngineeringPlugin.getInstance().getPreferenceStore();

        prefs.setDefault(ReleaseEngineeringPrefConstants.COUNT_BLANK_LINES, false);
        prefs.setDefault(ReleaseEngineeringPrefConstants.SUPPRESS_COMMON_PREFIX, true);
    }
}
