package net.tomato.component.exception;


/**
 * 为XmlUtil提供的RuntimeException<BR>
 * 
 * @author <a href="mailto:wanglei@primeton.com">Wang Lei</a>
 */

public class XmlUtilException extends RuntimeException {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 默认构造函数。<BR>
	 * 
	 * The default constructor.<BR>
	 */
	public XmlUtilException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public XmlUtilException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public XmlUtilException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public XmlUtilException(Throwable cause) {
		super(cause);
	}
}
