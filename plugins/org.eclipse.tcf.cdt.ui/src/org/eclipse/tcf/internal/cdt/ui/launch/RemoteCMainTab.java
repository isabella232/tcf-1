/*******************************************************************************
 * Copyright (c) 2011-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.launch;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.ui.CAbstractMainTab;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.internal.cdt.ui.Activator;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.ui.launch.PeerListControl;
import org.eclipse.tcf.internal.debug.ui.launch.PeerListControl.PeerInfo;
import org.eclipse.tcf.internal.debug.ui.launch.RemoteFileSelectionDialog;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.Preferences;

public class RemoteCMainTab extends CAbstractMainTab implements IShellProvider {

    private static final String REMOTE_PATH_DEFAULT = "";
    private static final boolean SKIP_DOWNLOAD_TO_REMOTE_DEFAULT = false;

    private PeerListControl fPeerList;
    private Text fRemoteProgText;
    private Button fRemoteBrowseButton;
    private Button fSkipDownloadButton;
    private boolean fIsInitializing = false;
    private PeerInfo fSelectedPeer;
    private boolean fPeerHasFileSystemService;
    private boolean fPeerHasProcessesService;

    @Override
    public String getName() {
        return "Main";
    }

    @Override
    public Shell getShell() {
        return super.getShell();
    }

    private String getRemoteWSRoot() {
        // TODO remoteWSRoot?
        return ""; //$NON-NLS-1$
    }

    @Override
    protected void handleSearchButtonSelected() {
    }

    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout topLayout = new GridLayout();
        setControl(comp);
        comp.setLayout(topLayout);

        /* TCF peer selection control */
        createPeerListGroup(comp);

        /* The Project and local binary location */
        createVerticalSpacer(comp, 1);
        createProjectGroup(comp, 1);
        createBuildConfigCombo(comp, 1);
        createExeFileGroup(comp, 1);

        /* The remote binary location and skip download option */
        createVerticalSpacer(comp, 1);
        createTargetExePathGroup(comp);
        createDownloadOption(comp);

        /* If the local binary path changes, modify the remote binary location */
        fProgText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                setLocalPathForRemotePath();
            }
        });

        PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(getControl(),
                        "org.eclipse.tcf.cdt.ui.remoteApplicationLaunchGroup"); //$NON-NLS-1$
    }

    protected void createExeFileGroup(Composite parent, int colSpan) {
        Composite mainComp = new Composite(parent, SWT.NONE);
        GridLayout mainLayout = new GridLayout();
        mainLayout.marginHeight = 0;
        mainLayout.marginWidth = 0;
        mainComp.setLayout(mainLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = colSpan;
        mainComp.setLayoutData(gd);
        fProgLabel = new Label(mainComp, SWT.NONE);
        fProgLabel.setText("C/C++ Application:");
        gd = new GridData();
        fProgLabel.setLayoutData(gd);
        fProgText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fProgText.setLayoutData(gd);
        fProgText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        Composite buttonComp = new Composite(mainComp, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonComp.setLayout(layout);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        buttonComp.setLayoutData(gd);
        buttonComp.setFont(parent.getFont());

        createVariablesButton(buttonComp, "&Variables...", fProgText);
        fSearchButton = createPushButton(buttonComp, "Searc&h Project...", null);
        fSearchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleSearchButtonSelected();
                updateLaunchConfigurationDialog();
            }
        });

        Button fBrowseForBinaryButton;
        fBrowseForBinaryButton = createPushButton(buttonComp, "B&rowse...", null);
        fBrowseForBinaryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleBinaryBrowseButtonSelected();
                updateLaunchConfigurationDialog();
            }
        });
    }


    private void createPeerListGroup(Composite comp) {
        new Label(comp, SWT.NONE).setText("Targets:");
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        prefs = prefs.node(RemoteCMainTab.class.getCanonicalName());
        fPeerList = new PeerListControl(comp, prefs);
        fPeerList.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handlePeerSelectionChanged();
                useDefaultsFromConnection();
                updateLaunchConfigurationDialog();
            }
        });
    }

    protected void handleBinaryBrowseButtonSelected() {
        FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
        fileDialog.setFileName(fProgText.getText());
        String text= fileDialog.open();
        if (text != null) fProgText.setText(text);
    }

    public boolean isValid(ILaunchConfiguration config) {
        boolean valid = super.isValid(config);
        if (valid) {
            if (fSelectedPeer == null) {
                setErrorMessage("No target selected.");
                valid = false;
            }
            else if (!fPeerHasProcessesService) {
                setErrorMessage("Selected target does not support 'Processes' service");
                valid = false;
            }
            if (valid) {
                String name = fRemoteProgText.getText().trim();
                if (name.length() == 0) {
                    setErrorMessage("Remote executable path is not specified.");
                    valid = false;
                }
            }
        }
        return valid;
    }

    protected void createTargetExePathGroup(Composite parent) {
        Composite mainComp = new Composite(parent, SWT.NONE);
        GridLayout mainLayout = new GridLayout();
        mainLayout.numColumns = 2;
        mainLayout.marginHeight = 0;
        mainLayout.marginWidth = 0;
        mainComp.setLayout(mainLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        mainComp.setLayoutData(gd);

        Label remoteProgLabel = new Label(mainComp, SWT.NONE);
        remoteProgLabel.setText("Remote Absolute File Path for C/C++ Application:");
        gd = new GridData();
        gd.horizontalSpan = 2;
        remoteProgLabel.setLayoutData(gd);

        fRemoteProgText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        fRemoteProgText.setLayoutData(gd);
        fRemoteProgText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        fRemoteBrowseButton = createPushButton(mainComp, "Browse...", null);
        fRemoteBrowseButton.setEnabled(fPeerHasFileSystemService);
        fRemoteBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                handleRemoteBrowseSelected();
                updateLaunchConfigurationDialog();
            }
        });
    }

    protected void createDownloadOption(Composite parent) {
        Composite mainComp = new Composite(parent, SWT.NONE);
        GridLayout mainLayout = new GridLayout();
        mainLayout.marginHeight = 0;
        mainLayout.marginWidth = 0;
        mainComp.setLayout(mainLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        mainComp.setLayoutData(gd);

        fSkipDownloadButton = createCheckButton(mainComp, "Skip download to target path.");
        fSkipDownloadButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });
        fSkipDownloadButton.setEnabled(fPeerHasFileSystemService);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, "");
        config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_COREFILE_PATH, "");
        config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_AUTO, true);

        config.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, false);
        config.setAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, false);
    }

    public void performApply(ILaunchConfigurationWorkingCopy config) {
        String peerId = getSelectedPeerId();
        if (peerId != null) {
            config.setAttribute(TCFLaunchDelegate.ATTR_PEER_ID, peerId);
        }
        config.setAttribute(TCFLaunchDelegate.ATTR_RUN_LOCAL_AGENT, false);
        config.setAttribute(TCFLaunchDelegate.ATTR_USE_LOCAL_AGENT, false);
        config.setAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, fRemoteProgText.getText());
        config.setAttribute(TCFLaunchDelegate.ATTR_COPY_TO_REMOTE_FILE, !fSkipDownloadButton.getSelection());
        super.performApply(config);
    }

    public void initializeFrom(ILaunchConfiguration config) {
        fIsInitializing = true;
        filterPlatform = getPlatform(config);
        updateProjectFromConfig(config);
        updateProgramFromConfig(config);
        updateBuildOptionFromConfig(config);
        updatePeerFromConfig(config);
        updateTargetProgFromConfig(config);
        updateSkipDownloadFromConfig(config);
        fIsInitializing = false;
    }

    protected void updatePeerFromConfig(ILaunchConfiguration config) {
        try {
            String peerId = config.getAttribute(TCFLaunchDelegate.ATTR_PEER_ID, (String) null);
            fPeerList.setInitialSelection(peerId);
        }
        catch (CoreException e) {
            // Ignore
        }
    }

    protected void handleRemoteBrowseSelected() {
        RemoteFileSelectionDialog dialog = new RemoteFileSelectionDialog(this, SWT.SAVE);
        dialog.setSelection(fRemoteProgText.getText().trim());
        dialog.setPeer(fSelectedPeer.peer);
        if (dialog.open() == Window.OK) {
            String file = dialog.getSelection();
            if (file != null) {
                fRemoteProgText.setText(file);
            }
        }
    }

    protected void updateTargetProgFromConfig(ILaunchConfiguration config) {
        String targetPath = REMOTE_PATH_DEFAULT;
        try {
            targetPath = config.getAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, REMOTE_PATH_DEFAULT);
        }
        catch (CoreException e) {
            // Ignore
        }
        fRemoteProgText.setText(targetPath);
    }

    protected void updateSkipDownloadFromConfig(ILaunchConfiguration config) {
        boolean doDownload = !SKIP_DOWNLOAD_TO_REMOTE_DEFAULT;
        try {
            doDownload = config.getAttribute(TCFLaunchDelegate.ATTR_COPY_TO_REMOTE_FILE, doDownload);
        }
        catch (CoreException e) {
            // Ignore for now
        }
        fSkipDownloadButton.setSelection(!doDownload);
    }

    /*
     * setLocalPathForRemotePath This function sets the remote path text field
     * with the value of the local executable path.
     */
    private void setLocalPathForRemotePath() {
        String programName = fProgText.getText().trim();
        boolean bUpdateRemote = false;

        String remoteName = fRemoteProgText.getText().trim();
        String remoteWsRoot = getRemoteWSRoot();
        if (remoteName.length() == 0) {
            bUpdateRemote = true;
        }
        else if (remoteWsRoot.length() != 0) {
            bUpdateRemote = remoteName.equals(remoteWsRoot);
        }

        if (programName.length() != 0 && bUpdateRemote && getCProject() != null) {
            IProject project = getCProject().getProject();
            IPath exePath = new Path(programName);
            if (!exePath.isAbsolute()) {
                exePath = project.getFile(programName).getLocation();

                IPath wsRoot = project.getWorkspace().getRoot().getLocation();
                exePath = makeRelativeToWSRootLocation(exePath, remoteWsRoot,
                        wsRoot);
            }
            String path = exePath.toString();
            fRemoteProgText.setText(path);
        }
    }

    private void useDefaultsFromConnection() {
        // During initialization, we don't want to use the default
        // values of the connection, but we want to use the ones
        // that are part of the configuration
        if (fIsInitializing) return;

        if ((fRemoteProgText != null) && !fRemoteProgText.isDisposed()) {
            String remoteName = fRemoteProgText.getText().trim();
            String remoteWsRoot = getRemoteWSRoot();
            if (remoteName.length() == 0) {
                fRemoteProgText.setText(remoteWsRoot);
            }
            else {
                // try to use remote path
                IPath wsRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation();
                IPath remotePath = makeRelativeToWSRootLocation(new Path(remoteName), remoteWsRoot, wsRoot);
                fRemoteProgText.setText(remotePath.toString());
            }
        }
        boolean hasFileSystemService = hasFileSystemService();
        if (fSkipDownloadButton != null && !fSkipDownloadButton.isDisposed()) {
            fSkipDownloadButton.setEnabled(hasFileSystemService);
        }
        if (fRemoteBrowseButton != null && !fRemoteBrowseButton.isDisposed()) {
            fRemoteBrowseButton.setEnabled(hasFileSystemService);
        }
    }

    private boolean hasFileSystemService() {
        return fPeerHasFileSystemService;
    }

    private void handlePeerSelectionChanged() {
        final PeerInfo info;
        ISelection selection = fPeerList.getSelection();
        if (selection instanceof IStructuredSelection) {
            info = (PeerInfo) ((IStructuredSelection) selection).getFirstElement();
        } else {
            info = null;
        }
        fSelectedPeer = info;
        fPeerHasFileSystemService = false;
        fPeerHasProcessesService = false;
        if (info != null) {
            Boolean[] available = new TCFTask<Boolean[]>() {
                public void run() {
                    final IChannel channel = info.peer.openChannel();
                    channel.addChannelListener(new IChannelListener() {
                        boolean opened;
                        public void congestionLevel(int level) {
                        }
                        public void onChannelClosed(final Throwable error) {
                            if (!opened) {
                                // TODO report error?
                                done(new Boolean[] { false, false });
                            }
                        }
                        public void onChannelOpened() {
                            opened = true;
                            Boolean hasFileSystemService = channel.getRemoteService(IFileSystem.class) != null;
                            Boolean hasProcessesService = channel.getRemoteService(IProcesses.class) != null;
                            channel.close();
                            done(new Boolean[] { hasFileSystemService, hasProcessesService });
                        }
                    });
                }
            }.getE();
            fPeerHasFileSystemService = available[0];
            fPeerHasProcessesService = available[1];
        }
        return;
    }

    private PeerInfo getSelectedPeer() {
        return fSelectedPeer;
    }

    private String getSelectedPeerId() {
        PeerInfo info = getSelectedPeer();
        if (info != null) {
            return info.id;
        }
        return null;
    }

    private IPath makeRelativeToWSRootLocation(IPath exePath,
            String remoteWsRoot, IPath wsRoot) {
        if (remoteWsRoot.length() != 0) {
            // use remoteWSRoot instead of Workspace Root
            if (wsRoot.isPrefixOf(exePath)) {
                return new Path(remoteWsRoot).append(exePath
                        .removeFirstSegments(wsRoot.segmentCount()).setDevice(
                                null));
            }
        }
        return exePath;
    }
}
