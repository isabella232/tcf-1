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

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.services.processes.IProcessService;
import org.eclipse.rse.subsystems.processes.core.subsystem.IHostProcessToRemoteProcessAdapter;
import org.eclipse.rse.subsystems.processes.servicesubsystem.ProcessServiceSubSystem;
import org.eclipse.rse.subsystems.processes.servicesubsystem.ProcessServiceSubSystemConfiguration;

public class TCFProcessSubSystemConfiguration extends ProcessServiceSubSystemConfiguration {
    
    private final TCFProcessAdapter process_adapter = new TCFProcessAdapter();

    @SuppressWarnings("unchecked")
    public Class getServiceImplType() {
        return TCFProcessService.class;
    }

    @Override
    public ISubSystem createSubSystemInternal(IHost host) {
        TCFConnectorService connectorService = (TCFConnectorService)getConnectorService(host);
        return new ProcessServiceSubSystem(host, connectorService,
                getProcessService(host), getHostProcessAdapter());
    }

    public IProcessService createProcessService(IHost host) {
        return new TCFProcessService(host);
    }

    public IHostProcessToRemoteProcessAdapter getHostProcessAdapter() {
        return process_adapter;
    }

    public IConnectorService getConnectorService(IHost host) {
        return TCFConnectorServiceManager.getInstance()
            .getConnectorService(host, ITCFSubSystem.class);
    }

    public void setConnectorService(IHost host, IConnectorService connectorService) {
        TCFConnectorServiceManager.getInstance().setConnectorService(host, getServiceImplType(), connectorService);
    }
}
