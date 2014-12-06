package org.iti.sqlschemacomparerplugin;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

public class PluginActivationContributionItem extends CompoundContributionItem {

	public PluginActivationContributionItem() {
		// TODO Auto-generated constructor stub
	}

	public PluginActivationContributionItem(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		Boolean isPluginActive = new Boolean(PluginUtils.isNatureActive(PluginUtils.getSelectedProject()));
		CommandContributionItemParameter param = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
				"org.iti.sqlschemacomparerplugin.pluginActivationContributionItem",
				"org.iti.sqlschemacomparerplugin.toggleNature",
				null,
				null,
				null,
				null,
				"SQL Schema Comparer",
				null,
				"(De-)Activate SQL Schema Comparer",
				CommandContributionItem.STYLE_CHECK,
				null,
				true);
		CommandContributionItem item = new CommandContributionItem(param);
		Command command = item.getCommand().getCommand();
		State state = command.getState("org.iti.sqlschemacomparerplugin.toggleNature.toggleState");

		state.setValue(isPluginActive);

		return new IContributionItem[] { item };
	}

}
