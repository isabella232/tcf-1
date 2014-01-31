/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Source provider for peer nodes and default context implementation.
 */
public class SourceProvider extends AbstractSourceProvider implements IEventListener, IPeerModelListener {

	/**
	 * Source name identifying the System Manager view selection.
	 */
	public static final String defaultContextSelectionName = "defaultContextSelection"; //$NON-NLS-1$

	/**
	 * Source name identifying the System Manager view selection.
	 */
	public static final String peerNodesName = "peerNodes"; //$NON-NLS-1$

	// The internal list of provided source names
	private final static String[] PROVIDED_SOURCE_NAMES = {defaultContextSelectionName, peerNodesName};

	// The reference to the expression evaluation service
	private IEvaluationService service = null;

	private IPeerNode defaultContext = null;
	private IPeerNode[] peerNodes = null;

	/**
     * Constructor.
     */
    public SourceProvider() {
    	super();
	    EventManager.getInstance().addEventListener(this, ChangeEvent.class);
	    ModelManager.getPeerModel().addListener(this);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.AbstractSourceProvider#initialize(org.eclipse.ui.services.IServiceLocator)
	 */
	@Override
	public void initialize(IServiceLocator locator) {
	    super.initialize(locator);

	    defaultContext = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);

	    peerNodes = ModelManager.getPeerModel().getPeerNodes();

	    // Register the source provider with the expression evaluation service
	    if (locator.hasService(IEvaluationService.class)) {
	    	service = (IEvaluationService)locator.getService(IEvaluationService.class);
	    	if (service != null) service.addSourceProvider(this);
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 */
	@Override
	public void dispose() {
	    EventManager.getInstance().removeEventListener(this);
	    ModelManager.getPeerModel().removeListener(this);

	    // Unregister the source provider from the expression evaluation service
	    if (service != null) { service.removeSourceProvider(this); service = null; }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getProvidedSourceNames()
	 */
	@Override
	public String[] getProvidedSourceNames() {
		return PROVIDED_SOURCE_NAMES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 */
	@Override
	public Map getCurrentState() {
		Map<String, Object> state = new HashMap<String, Object>();

		state.put(defaultContextSelectionName, defaultContext != null ? defaultContext : IEvaluationContext.UNDEFINED_VARIABLE);
		state.put(peerNodesName, peerNodes != null ? peerNodes : IEvaluationContext.UNDEFINED_VARIABLE);

		return state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
    @Override
    public void eventFired(EventObject event) {
    	if (event instanceof ChangeEvent) {
    		ChangeEvent changeEvent = (ChangeEvent)event;
    		if (changeEvent.getSource() instanceof IDefaultContextService || changeEvent.getSource() == defaultContext) {
    			defaultContext = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
    			fireSourceChanged(ISources.WORKBENCH, defaultContextSelectionName, defaultContext != null ? defaultContext : IEvaluationContext.UNDEFINED_VARIABLE);
    		}
    		if (changeEvent.getSource() instanceof IPeerNode && IPeerNodeProperties.PROP_CONNECT_STATE.equals(changeEvent.getEventId())) {
    			fireSourceChanged(ISources.WORKBENCH, peerNodesName, peerNodes != null ? peerNodes : IEvaluationContext.UNDEFINED_VARIABLE);
    		}
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
    @Override
    public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
		peerNodes = ModelManager.getPeerModel().getPeerNodes();
		fireSourceChanged(ISources.WORKBENCH, peerNodesName, peerNodes != null ? peerNodes : IEvaluationContext.UNDEFINED_VARIABLE);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
    @Override
    public void modelDisposed(IPeerModel model) {
    }
}