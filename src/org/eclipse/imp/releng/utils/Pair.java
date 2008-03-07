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