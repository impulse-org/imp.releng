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
package org.eclipse.imp.releng.utils;

public class Pair<T1,T2> {
    public final T1 first;
    public final T2 second;

    public Pair(T1 t1, T2 t2) {
        this.first= t1;
        this.second= t2;
    }

    @Override
    public String toString() {
        return "<" + first + "," + second + ">";
    }
}