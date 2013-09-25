/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.process;

import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.utils.Platform;
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ILineSeparatorConstants;
import org.eclipse.tcf.te.runtime.utils.Env;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.ui.terminals.process.activator.UIPlugin;
import org.eclipse.tcf.te.ui.terminals.process.nls.Messages;
import org.eclipse.tcf.te.ui.terminals.streams.AbstractStreamsConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsPage;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.osgi.framework.Bundle;

/**
 * Process connector implementation.
 */
@SuppressWarnings("restriction")
public class ProcessConnector extends AbstractStreamsConnector {
	// Reference to the process settings
	private final ProcessSettings settings;

	// Reference to the PTY instance.
	private PTY pty;
	// Reference to the launched process instance.
	private Process process;
	// Reference to the process monitor
	private ProcessMonitor monitor;

	// The terminal width and height. Initially unknown.
	private int width = -1;
	private int height = -1;

	/**
	 * Constructor.
	 */
	public ProcessConnector() {
		this(new ProcessSettings());
	}

	/**
	 * Constructor.
	 *
	 * @param settings The process settings. Must not be <code>null</code>
	 */
	public ProcessConnector(ProcessSettings settings) {
		super();

		Assert.isNotNull(settings);
		this.settings = settings;
	}

	/**
	 * Returns the process object or <code>null</code> if the
	 * connector is connector.
	 *
	 * @return The process object or <code>null</code>.
	 */
	public Process getProcess() {
		return process;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#connect(org.eclipse.tcf.internal.terminal.provisional.api.ITerminalControl)
	 */
	@Override
	public void connect(ITerminalControl control) {
		Assert.isNotNull(control);
		super.connect(control);

		pty = null;
		width = -1;
		height = -1;

		try {
			// Try to determine process and PTY instance from the process settings
			process = settings.getProcess();
			pty = settings.getPTY();

			// No process -> create PTY on supported platforms and execute
			// process image.
			if (process == null) {
				if (PTY.isSupported()) {
					try {
						// On Windows host, try to load the "WinPTY" extension.
						// Use reflection to make this work still with older CDT core packages
						if (Host.isWindowsHost()) {
							Bundle bundle = Platform.getBundle("org.eclipse.cdt.core"); //$NON-NLS-1$
							if (bundle != null) {
								try {
	                                Class<?> clazz = bundle.loadClass("org.eclipse.cdt.utils.pty.WinPTY"); //$NON-NLS-1$
	                                Constructor<?> constructor = clazz.getConstructor(boolean.class);
	                                pty = (PTY)constructor.newInstance(Boolean.FALSE);
                                }
                                catch (Exception e) { /* ignored on purpose */ }
							}
						} else {
							pty = new PTY(false);
						}
					} catch (IOException e) {
						// PTY not supported
					}
				}

				// Build up the command
				StringBuilder command = new StringBuilder(settings.getImage());
				String arguments = settings.getArguments();
				if (arguments != null && !"".equals(arguments.trim())) { //$NON-NLS-1$
					// Append to the command now
					command.append(" "); //$NON-NLS-1$
					command.append(arguments.trim());
				}

				File workingDir =null;
				if (settings.getWorkingDir()!=null){
					workingDir = new File(settings.getWorkingDir());
				}

				String[] envp = null;
				if (settings.getEnvironment()!=null){
					envp = settings.getEnvironment();
				}

				if (pty != null) {
					// A PTY is available -> can use the ProcessFactory.

					// Tokenize the command (ProcessFactory takes an array)
					StreamTokenizer st = new StreamTokenizer(new StringReader(command.toString()));
					st.resetSyntax();
					st.whitespaceChars(0, 32);
					st.whitespaceChars(0xa0, 0xa0);
					st.wordChars(33, 255);
					st.quoteChar('"');
					st.quoteChar('\'');

					List<String> argv = new ArrayList<String>();
					int ttype = st.nextToken();
					while (ttype != StreamTokenizer.TT_EOF) {
						argv.add(st.sval);
						ttype = st.nextToken();
					}

					// Execute the process
					process = ProcessFactory.getFactory().exec(argv.toArray(new String[argv.size()]), Env.getEnvironment(envp, true), workingDir, pty);
				} else {
					// No PTY -> just execute via the standard Java Runtime implementation.
					process = Runtime.getRuntime().exec(command.toString(), envp, workingDir);
				}
			}

			String lineSeparator = settings.getLineSeparator();
			if (lineSeparator == null) {
				lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
				if ("\r".equals(lineSeparator)) { //$NON-NLS-1$
					lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_CR;
				}
				else if ("\n".equals(lineSeparator)) { //$NON-NLS-1$
					lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_LF;
				}
				else {
					lineSeparator = ILineSeparatorConstants.LINE_SEPARATOR_CRLF;
				}
			}

			// Setup the listeners
			setStdoutListeners(settings.getStdOutListeners());
			setStderrListeners(settings.getStdErrListeners());

			// connect the streams
			connectStreams(control, process.getOutputStream(), process.getInputStream(), (pty == null ? process.getErrorStream() : null), settings.isLocalEcho(), lineSeparator);

			// Set the terminal control state to CONNECTED
			control.setState(TerminalState.CONNECTED);

			// Create the process monitor
			monitor = new ProcessMonitor(this);
			monitor.startMonitoring();
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
							NLS.bind(Messages.ProcessConnector_error_creatingProcess, e.getLocalizedMessage()), e);
			UIPlugin.getDefault().getLog().log(status);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#isLocalEcho()
	 */
	@Override
	public boolean isLocalEcho() {
		return settings.isLocalEcho();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#doDisconnect()
	 */
	@Override
	public void doDisconnect() {
		// Dispose the process
		if (process != null) { process.destroy(); process = null; }

		// Dispose the streams
		super.doDisconnect();

		// Set the terminal control state to CLOSED.
		fControl.setState(TerminalState.CLOSED);
	}

	// ***** Process Connector settings handling *****

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#makeSettingsPage()
	 */
	@Override
	public ISettingsPage makeSettingsPage() {
		return new ProcessSettingsPage(settings);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#getSettingsSummary()
	 */
	@Override
	public String getSettingsSummary() {
		return settings.getImage() != null ? settings.getImage() : ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#load(org.eclipse.tcf.internal.terminal.provisional.api.ISettingsStore)
	 */
	@Override
	public void load(ISettingsStore store) {
		settings.load(store);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#save(org.eclipse.tcf.internal.terminal.provisional.api.ISettingsStore)
	 */
	@Override
	public void save(ISettingsStore store) {
		settings.save(store);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#setTerminalSize(int, int)
	 */
	@Override
	public void setTerminalSize(int newWidth, int newHeight) {
		if (width != newWidth || height != newHeight) {
			width = newWidth;
			height = newHeight;
			if (pty != null) {
				pty.setTerminalSize(newWidth, newHeight);
			}
		}
	}

}
