package org.eurocarbdb.application.glycanbuilder.exception;

public class InvalidStringFormatException extends Exception {

	/*
   * 
   * eclipse generated serial id
   * 
   */
  private static final long serialVersionUID = 7094823642005342603L;

  public InvalidStringFormatException() {
	}

	/**
	 * @param message
	 */
	public InvalidStringFormatException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public InvalidStringFormatException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidStringFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InvalidStringFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
