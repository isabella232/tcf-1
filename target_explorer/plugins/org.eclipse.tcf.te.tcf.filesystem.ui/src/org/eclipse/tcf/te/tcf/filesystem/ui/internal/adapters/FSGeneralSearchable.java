/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.interfaces.ISearchMatcher;
import org.eclipse.tcf.te.ui.utils.AbstractSearchable;

/**
 * The searchable that provides a UI to collect and test
 * the general operations of a file search.
 */
public class FSGeneralSearchable extends AbstractSearchable {
	// The check option to define if system files should be searched.
	private Button fBtnSystem;
	// The check option to define if hidden files should be searched.
	private Button fBtnHidden;
	// The case sensitive check box.
	private Button fBtnCase;
	// The matching rule check box.
	private Button fBtnMatch;
	// The input field for searching conditions.
	private BaseEditBrowseTextControl fSearchField;
	// The current target names.
	private String fTargetName;
	// Whether it is case sensitive
	private boolean fCaseSensitive;
	// Whether it is precise matching.
	private boolean fMatchPrecise;
	// The flag if system files should be searched, default to true.
	private boolean fIncludeSystem = true;
	// The flag if hidden files should be searched, default to true.
	private boolean fIncludeHidden = true;
	// The types of target files.
	private Combo fCmbTypes;
	// The current selected target type index.
	private int fTargetType;
	// The root directory node.
	private FSTreeNode rootNode;
	
	/**
	 * Constructor
	 * 
	 * @param node the node whose sub tree will be searched.
	 */
	public FSGeneralSearchable(FSTreeNode node) {
		rootNode = node;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#createCommonPart(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createCommonPart(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout glayout = new GridLayout(3, false);
		glayout.marginHeight = 0;
		glayout.marginWidth = 0;
		composite.setLayout(glayout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Searching field.
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.FSGeneralSearchable_Find);

		fSearchField = new BaseEditBrowseTextControl(null);
		fSearchField.setIsGroup(false);
		fSearchField.setHasHistory(false);
		fSearchField.setHideBrowseButton(true);
		fSearchField.setParentControlIsInnerPanel(true);
		fSearchField.setupPanel(composite);
		fSearchField.setEditFieldValidator(new NameValidator());
		//fSearchField.setEditFieldValidator(new FolderValidator(this));
		Text text = (Text) fSearchField.getEditFieldControl();
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				searchTextModified();
			}
		});
		
		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChecked(e);
			}
		};
		
		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.FSGeneralSearchable_GeneralOptionText);
		group.setLayout(new GridLayout(2, true));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite cmpType = new Composite(group, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		cmpType.setLayoutData(data);
		cmpType.setLayout(new GridLayout(2, false));
		
		label = new Label(cmpType, SWT.NONE);
		label.setText(Messages.FSGeneralSearchable_FileType);
		
		// Search files only
		fCmbTypes = new Combo(cmpType, SWT.BORDER | SWT.READ_ONLY);
		fCmbTypes.setItems(new String[]{Messages.FSTreeNodeSearchable_FilesAndFolders, Messages.FSTreeNodeSearchable_FilesOnly, Messages.FSTreeNodeSearchable_FoldersOnly});
		fCmbTypes.setLayoutData(new GridData());
		fCmbTypes.addSelectionListener(l);
		
		// Case sensitive
		fBtnCase = new Button(group, SWT.CHECK);
		fBtnCase.setText(Messages.TreeViewerSearchDialog_BtnCaseText);
		data = new GridData();
		fBtnCase.setLayoutData(data);
		fBtnCase.addSelectionListener(l);

		// Matching precisely
		fBtnMatch = new Button(group, SWT.CHECK);
		fBtnMatch.setText(Messages.TreeViewerSearchDialog_BtnPreciseText);
		data = new GridData();
		fBtnMatch.setLayoutData(data);
		fBtnMatch.addSelectionListener(l);
		
		// If the target is Windows platform, then add system/hidden options.
		if(rootNode.isWindowsNode()) {
			fBtnSystem = new Button(group, SWT.CHECK);
			fBtnSystem.setText(Messages.FSGeneralSearchable_SearchSystemFiles);
			data = new GridData();
			fBtnSystem.setLayoutData(data);
			fBtnSystem.addSelectionListener(l);
			
			fBtnHidden = new Button(group, SWT.CHECK);
			fBtnHidden.setText(Messages.FSGeneralSearchable_SearchHiddenFiles);
			data = new GridData();
			fBtnHidden.setLayoutData(data);
			fBtnHidden.addSelectionListener(l);
		}
	}
	
	/**
	 * The text for searching is modified.
	 */
	protected void searchTextModified() {
		fireOptionChanged();
		fTargetName = fSearchField.getEditFieldControlText().trim();
    }
	
	/**
	 * Handling the event that a button is selected and checked.
	 * 
	 * @param e The selection event.
	 */
	protected void optionChecked(SelectionEvent e) {
		Object src = e.getSource();
		if (src == fBtnCase) {
			fCaseSensitive = fBtnCase.getSelection();
		}
		else if (src == fBtnMatch) {
			fMatchPrecise = fBtnMatch.getSelection();
		}
		else if (src == fCmbTypes) {
			fTargetType = fCmbTypes.getSelectionIndex();
		}
		else if (src == fBtnSystem) {
			fIncludeSystem = fBtnSystem.getSelection();
		}
		else if (src == fBtnHidden) {
			fIncludeHidden = fBtnHidden.getSelection();
		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#restoreValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
    public void restoreValues(IDialogSettings settings) {
		if(settings != null) {
			fCaseSensitive = settings.getBoolean("CaseSensitive"); //$NON-NLS-1$
			fBtnCase.setSelection(fCaseSensitive);
			fMatchPrecise = settings.getBoolean("MatchPrecise"); //$NON-NLS-1$
			fBtnMatch.setSelection(fMatchPrecise);
			try {
				fTargetType = settings.getInt("TargetType"); //$NON-NLS-1$
				fCmbTypes.select(fTargetType);
			}catch(NumberFormatException e) {
				fTargetType = 0;
			}
			fTargetName = settings.get("TargetName"); //$NON-NLS-1$
			if (fTargetName != null) {
				fSearchField.setEditFieldControlText(fTargetName);
			}
			if (rootNode.isWindowsNode()) {
				fIncludeSystem = settings.get("IncludeSystem") == null ? true : settings.getBoolean("IncludeSystem"); //$NON-NLS-1$ //$NON-NLS-2$
				fIncludeHidden = settings.get("IncludeHidden") == null ? true : settings.getBoolean("IncludeHidden"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			fCaseSensitive = false;
			fMatchPrecise = false;
			fTargetType = 0;
			fTargetName = null;
			if(rootNode.isWindowsNode()) {
				fIncludeHidden = true;
				fIncludeSystem = true;
			}
		}
		fBtnCase.setSelection(fCaseSensitive);
		fBtnMatch.setSelection(fMatchPrecise);
		fCmbTypes.select(fTargetType);
		if (fTargetName != null) {
			fSearchField.setEditFieldControlText(fTargetName);
		}
		if (rootNode.isWindowsNode()) {
			fBtnSystem.setSelection(fIncludeSystem);
			fBtnHidden.setSelection(fIncludeHidden);
		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#persistValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
    public void persistValues(IDialogSettings settings) {
		if(settings != null) {
			settings.put("CaseSensitive", fCaseSensitive); //$NON-NLS-1$
			settings.put("MatchPrecise", fMatchPrecise); //$NON-NLS-1$
			settings.put("TargetType", fTargetType); //$NON-NLS-1$
			settings.put("TargetName", fTargetName); //$NON-NLS-1$
			if(rootNode.isWindowsNode()) {
				settings.put("IncludeSystem", fIncludeSystem); //$NON-NLS-1$
				settings.put("IncludeHidden", fIncludeHidden); //$NON-NLS-1$
			}
		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getMatcher()
	 */
	@Override
	public ISearchMatcher getMatcher() {
		return new FSTreeNodeMatcher(fCaseSensitive, fMatchPrecise, fTargetType, fTargetName, fIncludeSystem, fIncludeHidden);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#isInputValid()
	 */
	@Override
	public boolean isInputValid() {
		return fSearchField.isValid();
	}
}
