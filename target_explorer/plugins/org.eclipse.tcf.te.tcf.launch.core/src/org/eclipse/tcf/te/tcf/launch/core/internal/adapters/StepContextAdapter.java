/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal.adapters;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;

/**
 * Peer model step context adapter implementation.
 */
public class StepContextAdapter extends PlatformObject implements IStepContext {
	// Reference to the launch
	private final ILaunch launch;

	/**
	 * Constructor.
	 *
	 * @param launch The launch. Must not be <code>null</code>.
	 */
	public StepContextAdapter(ILaunch launch) {
		super();
		Assert.isNotNull(launch);
		this.launch = launch;
	}

	/**
	 * Returns the launch.
	 * @return The launch.
	 */
	public ILaunch getLaunch() {
		return launch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getId()
	 */
	@Override
	public String getId() {
		return launch != null && launch.getLaunchConfiguration() != null ? launch.getLaunchConfiguration().getName() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getSecondaryId()
	 */
	@Override
	public String getSecondaryId() {
		return launch != null ? launch.getLaunchMode() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getName()
	 */
	@Override
	public String getName() {
		return getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getContextObject()
	 */
	@Override
	public Object getContextObject() {
		return launch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getInfo(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public String getInfo(IPropertiesContainer data) {
		try {
			return getName() + "(" + launch.getLaunchMode() + ") - " + launch.getLaunchConfiguration().getType().getName(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException e) {
		}
		return getName() + "(" + launch.getLaunchMode() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		if (ILaunch.class.equals(adapter)) {
			return launch;
		}

		if (ILaunchConfiguration.class.isAssignableFrom(adapter)) {
			return launch.getLaunchConfiguration();
		}

		if (ILaunchConfigurationType.class.isAssignableFrom(adapter)) {
			try {
				return launch.getLaunchConfiguration().getType();
			}
			catch (CoreException e) {
			}
		}

		return super.getAdapter(adapter);
	}
}
