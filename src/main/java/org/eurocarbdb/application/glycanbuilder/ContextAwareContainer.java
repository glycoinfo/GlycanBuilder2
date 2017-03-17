package org.eurocarbdb.application.glycanbuilder;

public interface ContextAwareContainer {
	public void fireContextChanged(Context context,boolean switchToDefault);

	public void fireUndoContextChanged(Context context);
}
