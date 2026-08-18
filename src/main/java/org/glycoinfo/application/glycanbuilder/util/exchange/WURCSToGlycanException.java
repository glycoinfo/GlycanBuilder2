package org.glycoinfo.application.glycanbuilder.util.exchange;

import org.glycoinfo.WURCSFramework.util.WURCSException;

public class WURCSToGlycanException extends WURCSException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String badResidueMessage = "Failed to convert residue";
	public static final String badSubstituentMessage = "Failed to convert substituent";

	public WURCSToGlycanException(String a_strMessage, Throwable a_oCause) {
		super(a_strMessage, a_oCause);
	}

	public WURCSToGlycanException(String a_strMessage) {
		super(a_strMessage);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public WURCSToGlycanException(String a_strMessage, String seq, Throwable a_oCause) {
		super(a_strMessage + ": " + seq, a_oCause);
	}

	public WURCSToGlycanException(String a_strMessage, String seq) {
		super(a_strMessage + ": " + seq);
	}

}

