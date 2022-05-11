package life.genny.qwandaq;

import java.lang.Override;

public enum VisualControlType {
	VCL_DEFAULT ("VCL_DEFAULT"),
	VCL_WRAPPER ("VCL_WRAPPER"),
	VCL_INPUT ("VCL_INPUT"),
	VCL_ICON ("VCL_ICON"),
	VCL_LABEL ("VCL_LABEL"),
	VCL_DESCRIPTION ("VCL_DESCRIPTION"),
	VCL_HINT ("VCL_HINT"),
	VCL_ERROR ("VCL_ERROR"),
	VCL_REQUIRED ("VCL_REQUIRED"),
	VCL_DELIMITER ("VCL_DELIMITER"),
	INPUT_WRAPPER ("INPUT_WRAPPER"),
	INPUT_FIELD ("INPUT_FIELD"),
	INPUT_ICON ("INPUT_ICON"),
	INPUT_ITEM_WRAPPER ("INPUT_ITEM_WRAPPER"),
	INPUT_ITEM ("INPUT_ITEM"),
	INPUT_SELECTED_WRAPPER ("INPUT_SELECTED_WRAPPER"),
	INPUT_SELECTED ("INPUT_SELECTED"),
	INPUT_PLACEHOLDER ("INPUT_PLACEHOLDER"),
	INPUT_BUTTON ("INPUT_BUTTON"),
	GROUP ("GROUP"),
	VCL ("VCL"),
	GROUP_WRAPPER ("GROUP_WRAPPER"),
	GROUP_LABEL ("GROUP_LABEL"),
	GROUP_DESCRIPTION ("GROUP_DESCRIPTION"),
	GROUP_INPUT ("GROUP_INPUT"),
	GROUP_HEADER_WRAPPER ("GROUP_HEADER_WRAPPER"),
	GROUP_CLICKABLE_WRAPPER ("GROUP_CLICKABLE_WRAPPER"),
	GROUP_ICON ("GROUP_ICON"),
	GROUP_CONTENT_WRAPPER ("GROUP_CONTENT_WRAPPER");

	private final String name;

	private VisualControlType(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		// (otherName == null) check is not needed because name.equals(null) returns
		// false
		return name.equals(otherName);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
