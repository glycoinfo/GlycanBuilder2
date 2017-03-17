/**
 * 
 */
package org.eurocarbdb.application.glycanbuilder;

public enum ICON_SIZE {
	L1(16),
	L2(22),
	L3(24),
	L4(32),
	L5(48),
	L6(64),
	L7(128),
	L0(0);
	
	
	private int size;
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	private ICON_SIZE(int size){
		this.size=size;
	}
}