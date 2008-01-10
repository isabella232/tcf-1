/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package com.windriver.tcf.rse.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import com.windriver.tcf.api.util.TCFTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.services.clientserver.messages.IndicatorException;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;

public abstract class TCFRSETask<V> extends TCFTask<V> {
    
    public V get(IProgressMonitor monitor, String task_name)
            throws InterruptedException, ExecutionException {
        monitor.beginTask(task_name, 1);
        try {
            return get();
        }
        finally {
            monitor.done();
        }
    }
        
    public V getS(IProgressMonitor monitor, String task_name) throws SystemMessageException {
        if (monitor != null) monitor.beginTask(task_name, 1);
        try {
            return get();
        }
        catch (Throwable e) {
            if (e instanceof SystemMessageException) throw (SystemMessageException)e;
            try {
                SystemMessage m = new SystemMessage("TCF", "C", "0001", SystemMessage.ERROR,
                        e.getClass().getName(), e.getMessage());
                throw new SystemMessageException(m);
            }
            catch (IndicatorException e1) {
                throw new Error(e1);
            }
        }
        finally {
            if (monitor != null) monitor.done();
        }
    }

    public V getI(IProgressMonitor monitor, String task_name) throws InvocationTargetException, InterruptedException {
        if (monitor != null) monitor.beginTask(task_name, 1);
        try {
            return get();
        }
        catch (Throwable e) {
            if (e instanceof InvocationTargetException) throw (InvocationTargetException)e;
            if (e instanceof InterruptedException) throw (InterruptedException)e;
            throw new InvocationTargetException(e);
        }
        finally {
            if (monitor != null) monitor.done();
        }
    }
}
