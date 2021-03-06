/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.developerstudio.datamapper.diagram.custom.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.common.ui.action.AbstractActionHandler;
import org.eclipse.gmf.runtime.diagram.ui.editparts.GraphicalEditPart;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.wso2.developerstudio.datamapper.DataMapperPackage;
import org.wso2.developerstudio.datamapper.TreeNode;
import org.wso2.developerstudio.datamapper.diagram.custom.util.AddNewTypeDialog;
import org.wso2.developerstudio.datamapper.diagram.edit.parts.InputEditPart;
import org.wso2.developerstudio.datamapper.diagram.edit.parts.OutputEditPart;
import org.wso2.developerstudio.datamapper.diagram.edit.parts.TreeNode2EditPart;
import org.wso2.developerstudio.datamapper.diagram.edit.parts.TreeNode3EditPart;
import org.wso2.developerstudio.datamapper.diagram.edit.parts.TreeNodeEditPart;
import org.wso2.developerstudio.eclipse.registry.core.interfaces.IRegistryFile;

public class EditRecordAction extends AbstractActionHandler {

	private EditPart selectedEP;
	private static final String OUTPUT_EDITPART = "Output"; //$NON-NLS-1$
	private static final String INPUT_EDITPART = "Input"; //$NON-NLS-1$
	private static final String RENAME_ACTION_ID = "rename-node-action-id"; //$NON-NLS-1$
	private static final String RENAME_FIELD = Messages.EditActions_editNode;

	private static final String NAME = "name";
	private static final String PREFIX = "prefix";
	private static final String DOC = "doc";
	private static final String ALAISES = "aliased";
	private static final String SCHEMATYPE = "schemaType";

	private String name;
	private String schemaType;
	private String aliases;
	private String prefix;
	private String namespace;

	public EditRecordAction(IWorkbenchPart workbenchPart) {
		super(workbenchPart);

		setId(RENAME_ACTION_ID);
		setText(RENAME_FIELD);
		setToolTipText(RENAME_FIELD);
		ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		selectedEP = getSelectedEditPart();

		if (null != selectedEP) {
			// Returns the TreeNodeImpl object respective to selectedEP
			EObject object = ((Node) selectedEP.getModel()).getElement();
			// Used to identify the selected resource of the model
			TreeNode selectedNode = (TreeNode) object;

			name = selectedNode.getName();
			schemaType = selectedNode.getSchemaDataType().toString();
			if (selectedNode.getAliases().isEmpty()) {
				aliases = null;
			} else {
				aliases = selectedNode.getAliases().toString().replace("[", "").replace("]", "");
			}
			prefix = selectedNode.getNamespace();
			namespace = selectedNode.getDoc();
			HashMap<String, String> map = openEditRecordDialog(name, prefix, schemaType, namespace, aliases);

			Set<String> aliasesMap = getAliasesValue(map);

			if (map.get(NAME) != null && map.get(DOC) != null && map.get(SCHEMATYPE) != null && map.get(PREFIX) != null && map.get(ALAISES) != null) {
				// Serialize the values
				executeCommand(selectedNode, DataMapperPackage.Literals.TREE_NODE__NAME, map.get(NAME));
				executeCommand(selectedNode, DataMapperPackage.Literals.TREE_NODE__NAMESPACE, map.get(PREFIX));
				executeCommand(selectedNode, DataMapperPackage.Literals.TREE_NODE__SCHEMA_DATA_TYPE,
						map.get(SCHEMATYPE));
				executeCommand(selectedNode, DataMapperPackage.Literals.TREE_NODE__DOC, map.get(DOC));

				// Serialize the aliases
				SetCommand renameComd = new SetCommand(((GraphicalEditPart) selectedEP).getEditingDomain(),
						selectedNode, DataMapperPackage.Literals.TREE_NODE__ALIASES, aliasesMap);
				if (renameComd.canExecute()) {
					((GraphicalEditPart) selectedEP).getEditingDomain().getCommandStack().execute(renameComd);

				}

				// Sets the name with prefix in the tree view
				if (getSelectedEditPart() instanceof TreeNode2EditPart) {
					((TreeNode2EditPart) getSelectedEditPart())
							.renameElementItem(map.get(PREFIX) + ":" + map.get(NAME));
				} else if (getSelectedEditPart() instanceof TreeNode2EditPart) {
					((TreeNode2EditPart) getSelectedEditPart())
							.renameElementItem(map.get(PREFIX) + ":" + map.get(NAME));
				} else if (getSelectedEditPart() instanceof TreeNode3EditPart) {
					((TreeNode3EditPart) getSelectedEditPart())
							.renameElementItem(map.get(PREFIX) + ":" + map.get(NAME));
				}
			}
		}
	}

	/**
	 * Gets the aliases values as a Set
	 * 
	 * @param map
	 *            value map
	 * @return Set
	 */
	private Set<String> getAliasesValue(HashMap<String, String> map) {
		Set<String> aliasesMap = null;
		if (map.get(ALAISES) != null) {
			String values = map.get(ALAISES);
			aliasesMap = new HashSet<String>(Arrays.asList(values.split("\\s*,\\s*")));
		}
		return aliasesMap;
	}

	/**
	 * Save edited values into the model
	 * 
	 * @param selectedNode
	 *            node
	 * @param String
	 *            value
	 */
	private void executeCommand(TreeNode selectedNode, EStructuralFeature feature, String value) {
		if (StringUtils.isNotEmpty(value)) {
			SetCommand editComd = new SetCommand(((GraphicalEditPart) selectedEP).getEditingDomain(), selectedNode,
					feature, value);
			if (editComd.canExecute()) {
				((GraphicalEditPart) selectedEP).getEditingDomain().getCommandStack().execute(editComd);
			}
		}
	}

	/**
	 * Opens the dialog
	 * 
	 * @param name
	 *            name
	 * @param prefix
	 *            prefix
	 * @param schemaType
	 *            schemaType
	 * @param namespace
	 *            namespace
	 * @param aliases
	 *            aliases
	 * @return map
	 */
	private HashMap<String, String> openEditRecordDialog(String name, String prefix, String schemaType,
			String namespace, String aliases) {
		String newName = null;
		String newPrefix = null;
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		AddNewTypeDialog editTypeDialog = new AddNewTypeDialog(shell, new Class[] { IRegistryFile.class });
		editTypeDialog.create();
		if (StringUtils.isNotEmpty(name)) {
			if (name.contains(":")) {
				String[] fullName = name.split(":");
				newName = fullName[1];
				newPrefix = fullName[0];
			} else {
				newName = name;
				newPrefix = prefix;
			}
		}

		editTypeDialog.setValues(newName, newPrefix, schemaType, namespace, aliases);
		editTypeDialog.open();

		HashMap<String, String> valueMap = new HashMap<String, String>();

		if (StringUtils.isNotEmpty(editTypeDialog.getName())) {
			if (editTypeDialog.getName().contains(":")) {
				String[] fullName = editTypeDialog.getName().split(":");
				valueMap.put(NAME, fullName[1]);
				valueMap.put(PREFIX, fullName[0]);
			} else {
				valueMap.put(NAME, editTypeDialog.getName());
				valueMap.put(PREFIX, null);
			}
		}
		valueMap.put(SCHEMATYPE, editTypeDialog.getSchemaType());
		if (StringUtils.isNotEmpty(editTypeDialog.getDoc())) {
			valueMap.put(DOC, editTypeDialog.getDoc());
		}
		if (editTypeDialog.getAliases() != null) {
			valueMap.put(ALAISES, editTypeDialog.getAliases().toString().replace("[", "").replace("]", ""));
		}
		return valueMap;

	}

	private String getSelectedInputOutputEditPart() {
		EditPart tempEP = selectedEP;
		while (!(tempEP instanceof InputEditPart) && !(tempEP instanceof OutputEditPart)) {
			tempEP = tempEP.getParent();
		}

		if (tempEP instanceof InputEditPart) {
			return INPUT_EDITPART;
		} else if (tempEP instanceof OutputEditPart) {
			return OUTPUT_EDITPART;
		} else {
			// When the selected editpart is not InputEditPart or OutputEditPart
			return null;
		}
	}

	private EditPart getSelectedEditPart() {
		IStructuredSelection selection = getStructuredSelection();
		if (selection.size() == 1) {
			Object selectedEP = selection.getFirstElement();
			if (selectedEP instanceof EditPart) {
				return (EditPart) selectedEP;
			}
		}
		// In case of selecting the wrong editpart
		return null;
	}

	@Override
	public void refresh() {
		// refresh action. Does not do anything
	}

}
