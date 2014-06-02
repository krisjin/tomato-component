package net.tomato.component;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class NodeDescription {
	class Pair {
		public String first;
		public String second;

		public Pair(String strFirst, String strSecond) {
			first = strFirst;
			second = strSecond;
		}
	}

	public static final short NODE_ELEMENT = 0;
	public static final short NODE_ATTR = 1;
	public static final short NODE_ANY = 2;

	// public Attr m_attr = null;
	private String m_strNodeName = "";
	private ArrayList<Pair> m_alAttrs = new ArrayList<Pair>();
	private short m_type = 0;

	public NodeDescription(String xPathItem) {
		// in there,only three styles 'step' are supported,one is '*',
		// one is '@attrname' and last one is
		// 'nodename[@attrname=attrval][...]...'
		if (xPathItem.indexOf("*") == 0)
			m_type = NODE_ANY;
		else if (xPathItem.indexOf("@") == 0) {
			m_strNodeName = "";
			m_type = NODE_ATTR;
			StringTokenizer stk = new StringTokenizer(xPathItem, "@");
			if (stk.hasMoreTokens())
				m_alAttrs.add(new Pair(stk.nextToken(), ""));
		} else {
			m_type = NODE_ELEMENT;
			int index = xPathItem.indexOf("[@");
			if (index == -1) {
				m_strNodeName = xPathItem;
			} else {
				m_strNodeName = xPathItem.substring(0, index);
				xPathItem = xPathItem.substring(index);

				// 是否在双引号内
				boolean inDoubleIngo = false;
				// 是否在单引号内
				boolean inSingeIngo = false;
				// 是否在\之后
				boolean afterBacklash = false;
				// 是否在=之后
				boolean afterEqual = false;

				StringBuffer bufName = new StringBuffer();
				StringBuffer bufValue = new StringBuffer();
				for (int i = 0; i < xPathItem.length(); i++) {
					char c = xPathItem.charAt(i);
					if (afterBacklash) {
						if (afterEqual) {
							bufValue.append(c);
						}
						afterBacklash = false;
						continue;
					}
					if (c == '\\') {
						if (afterEqual) {
							bufValue.append(c);
						}
						afterBacklash = true;
						continue;
					}

					if (c == '"') {
						inDoubleIngo = !inDoubleIngo;
						continue;
					}
					if (c == '\'') {
						inSingeIngo = !inSingeIngo;
						continue;
					}
					if (c == '=') {
						if (!inDoubleIngo && !inSingeIngo) {
							afterEqual = true;
							continue;
						}
					}
					if (c == '[' || c == '@') {
						if (!inDoubleIngo && !inSingeIngo) {
							afterEqual = false;
							continue;
						}
					}
					if (c == ']') {
						if (!inDoubleIngo && !inSingeIngo) {
							m_alAttrs.add(new Pair(bufName.toString(), bufValue.toString()));

							inDoubleIngo = false;
							inSingeIngo = false;
							afterBacklash = false;
							afterEqual = false;

							bufName = new StringBuffer();
							bufValue = new StringBuffer();
							continue;
						}
						if (afterEqual) {
							bufValue.append(c);
						}
						continue;
					}

					if (afterEqual) {
						bufValue.append(c);
					} else {
						bufName.append(c);
					}
				}
			}

			// StringTokenizer stk = new StringTokenizer(xPathItem,"[@=\"']");
			// if( stk.hasMoreTokens() ) m_strNodeName = stk.nextToken();
			// while( stk.hasMoreTokens() ){
			// String strAttrName = stk.hasMoreTokens()?stk.nextToken():"";
			// String strAttrVal = stk.hasMoreTokens()?stk.nextToken():"";
			// m_alAttrs.add(new Pair(strAttrName,strAttrVal));
			// }
		}
	}

	/**
	 * description: compare to an element.
	 * 
	 * @param ele
	 *            --the element to compare with this NodeDescription
	 * @return boolean --true if ele and NodeDescription are equal. false is
	 *         otherwise.
	 */
	public boolean equals(Element ele) {
		if (ele == null)
			return false;

		// if 'xPathItem' describe an element,then compare it's name and
		// attribute to ele's (argument) name and attributes.
		// if 'xPathItem' describe an attribue,then check it(attribute) exist in
		// ele
		switch (m_type) {
		case NODE_ELEMENT:
			if (ele.getNodeName() == null)
				return false;
			if (!ele.getNodeName().equals(m_strNodeName))
				return false;
			for (int i = 0; i < m_alAttrs.size(); i++) {
				String strAttrName = ((Pair) m_alAttrs.get(i)).first;
				String strAttrVal = ((Pair) m_alAttrs.get(i)).second;

				// if style like 'nodename[@attrname]',means to find a node
				// which has 'attrname' attribute
				if (strAttrVal.equals(""))
					return ele.hasAttribute(strAttrName);
				// if style like 'nodename[@attrname=attrval]',means to find
				// a node which has 'attrname' attribute,and it's value equal
				// to 'attrval'
				if (!strAttrVal.equals(ele.getAttribute(strAttrName)))
					return false;
			}
			break;
		case NODE_ATTR:
			String strAttrName = ((Pair) m_alAttrs.get(0)).first;
			if (!ele.hasAttribute(strAttrName))
				return false;
			// m_attr = ele.getAttributeNode(strAttrName);
			break;
		case NODE_ANY:
		default:
		}
		return true;
	}

	public short getType() {
		return m_type;
	}

	public Attr getAttribute(Element ele) {
		String strAttrName = ((Pair) m_alAttrs.get(0)).first;
		return ele.getAttributeNode(strAttrName);
	}
}
