/*******************************************************************************
 * Copyright (c) 2012 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.ui;

import org.eclipse.debug.core.Launch;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.protocol.IChannel;

/**
 * TCFModel represents remote target state as it is known to host.
 * The main job of the model is caching remote data,
 * keeping the cache in a coherent state,
 * and feeding UI with up-to-date data.
 */
public interface ITCFModel {

    /**
     * Get TCF communication channel that is used by this model.
     * @return IChannel interface.
     */
    IChannel getChannel();
    
    /**
     * Get display that is used by this model.
     * @return Display object.
     */
    Display getDisplay();
    
    /**
     * Get launch object associated with this model.
     * @return Launch object.
     */
    Launch getLaunch();
    
    /**
     * Get model object with given ID.
     * @param id - TCF ID of remote object.
     * @return ITCFObject interface or null - if model does not include such object.
     * Note that the model is built lazily (on demand): getNode() can return null even if
     * "id" is valid ID of existing remote object. 
     */
    ITCFObject getNode(String id);
}
