package net.tomato.component;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import net.tomato.compontent.exception.XmlUtilException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * XML处理工具类<BR>
 *
 */

public final class XmlUtil {

	/**
	 * XML相关定义
	 */
	// XPath Constants
	static final String XPATH_NODE_DELIM = "/";

	static final String XPATH_ATTR_DELIM = "@";

	static final String XPATH_ATTR_LINCL = "[";

	static final String XPATH_ATTR_RINCL = "]";

	static final String XPATH_ATTR_QUOTA = "\"";

	static final String XPATH_ATTR_SQUOTA = "'";

	static final String XPATH_ATTR_EQ = "=";

	// private static final String XPATH_SPACE_CHAR = "\t\n\r";

	/** UTF-8 xml编码方式 */
	public final static String UTF_XML_ENCODING = "UTF-8";

	final static String ENCODING_STR = "encoding";

	/** GBK xml编码方式 */
	public static final String GBK_XML_ENCODING = "GBK";

	private static final String TAB_SPACE = "4";

	//	space符号
	private static char[] chars = {
		'n',
		(char) 9,
		(char) 10,
		(char) 13,
		' '
	};

	/**
	 * 因为不需要实例，所以构造函数为私有<BR>
	 * 参见Singleton模式<BR>
	 *
	 * Only one instance is needed,so the default constructor is private<BR>
	 * Please refer to singleton design pattern.
	 */
	private XmlUtil() {
		super();
	}

	/**
	 * 搜索 xml 文件头
	 *
	 * @param fileName
	 *            文件名称
	 *
	 * @return 文件头
	 */
	private static String fileXMLHeader(String fileName) {
		BufferedReader in = null;
		FileInputStream input = null;
		try {
			input = new FileInputStream(fileName);
			in = new BufferedReader(new InputStreamReader(input, UTF_XML_ENCODING));
			String header = in.readLine();
			if (header == null) {
				return null;
			}
			else {
				header = header.trim();
			}
			if (header.startsWith("<?") && header.endsWith("?>")) {
				return header;
			}
			else {
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(input);
		}
	}

	/**
	 * 判断 xml 头中的 encoding
	 *
	 * @param header
	 * @return
	 */
	private static String xmlEncoding(String header) {
		if ((header == null) || header.equals("")) {
			return null;
		}

		int op = header.indexOf(ENCODING_STR);
		if (op == -1) {
			return null;
		}
		op = header.indexOf("\"", op);
		if (op == -1) {
			return null;
		}

		String tmp = header.substring(op + "\"".length());
		op = tmp.indexOf("\"");
		if (op == -1) {
			return null;
		}
		tmp = tmp.substring(0, op);
		return tmp.trim();
	}

	/**
	 * 搜索 xml String 头
	 *
	 * @param xml
	 * @return
	 */
	private static String stringXMLHeader(String xml) {
		int start = xml.indexOf("<?");
		if ((start != -1) && (xml.substring(0, start).trim().length() <= 2)) {
			// 后一个子式之所以取2是为了兼容Unicode的BOM头,虽然parse未必支持BOM,但是在这里仍然写的健壮些
			int end = xml.indexOf("?>", start);
			if (end != -1) {
				return xml.substring(start, end + 2);
				// end + 2 刚好将?>包括进去了
			}
		}
		return null;
	}

	/** 实例化XML Document
	 *
	 * @return Document
	 * @throws XmlUtilException
	 */
	public static Document newDocument() throws XmlUtilException {
		return getXMLBuilder().newDocument();
	}

	/**
	 * 获得一个new Document Instance
	 * @return Document
	 * @throws XmlUtilException
	 * @Deprecated see {@link #newDocument()}
	 */
	@Deprecated
	public final static Document getNewDocument() throws XmlUtilException {
		Document doc = getXMLBuilder().newDocument();
		return doc;
	}

	/**
	 * to get node's owner document
	 *
	 * @return The document that contain node
	 */
	public static Document getOwnerDocument(Node node) {
		Document doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) node;
		}
		else {
			doc = node.getOwnerDocument();
		}
		return doc;
	}

	/**
	 * set a document root node, if root node already exist,replace it
	 *
	 * @param doc
	 *            a document
	 * @param rootName
	 *            the root node name
	 * @return The document that is parameter 'doc' after processed
	 */
	public final static Document setRoot(Document doc, String rootName) {
		Node root = doc.getDocumentElement();
		if (root != null) {
			doc.removeChild(root);
		}
		doc.appendChild(doc.createElement(rootName));
		return doc;
	}

	/**
	 * set a document root node, if root node already exist, replace it
	 *
	 * @param doc
	 *            a document
	 * @param root
	 *            the root node
	 * @return The document that is parameter 'doc' after processed.
	 */
	public final static Document setRoot(Document doc, Node root) {
		Node oldRoot = doc.getDocumentElement();
		if (oldRoot != null) {
			doc.removeChild(oldRoot);
		}
		doc.appendChild(root);
		return doc;
	}

	/**
	 * 得到根结点。<BR>
	 *
	 * @param doc
	 * @return
	 */
	public final static Element getRoot(Document doc) {
		return doc.getDocumentElement();
	}

	/**
	 * 得到XML文件在文件头指定的编码格式。<BR>
	 *
	 * @param fileName
	 * @return
	 */
	public final static String getXmlEncoding(String fileName) {
		String header = fileXMLHeader(fileName);
		return xmlEncoding(header);
	}

	/**
	 * 为某个节点添加特定属性并设定值
	 * @param doc
	 * @param atNode     要挂靠的目标节点
	 * @param attrName   属性名称
	 * @param attrValue  属性值
	 */
	public static void createAttr(Document doc, Node atNode, String attrName, String attrValue) {
		Attr newAttr = doc.createAttribute(attrName);
		newAttr.setNodeValue(attrValue);
		atNode.getAttributes().setNamedItem(newAttr);
	}

	/**
	 * 建立一个新的Element
	 *
	 * @param Document
	 *            doc
	 * @param String
	 *            name
	 * @param Object
	 *            value
	 *
	 * @return Element
	 */
	public static Element createElement(Document doc, String name, Object value) {
		if (name == null) {
			return null;
		}
		Element element = doc.createElement(name);
		if (value != null) {
			setElementValue(element, value);
		}
		return element;
	}

	/**
	 * 为Element赋值
	 *
	 * @param Element
	 *            ele
	 * @param Object
	 *            value
	 */
	public static void setElementValue(Element ele, Object value) {
		if (value == null) {
			return;
		}
		setNodeValue(ele, value.toString());
	}

	/**
	 * 取得指定节点
	 *
	 * @param parentNode
	 *            父节点
	 * @param nodeName
	 *            节点名称
	 *
	 * @return 节点
	 */
	public static Element getElement(Element parentNode, String nodeName) {
		if (parentNode == null) {
			return null;
		}
		NodeList nl = parentNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				if (nl.item(i).getNodeName().equals(nodeName)) {
					return (Element) nl.item(i);
				}
			}
		}
		return null;
	}

	/**
	 * 根据指定名称, 创建一个子节点
	 *
	 * @param anElement
	 * @param name
	 * @return
	 */
	public static Element createField(Element anElement, String name) {
		Element field = anElement.getOwnerDocument().createElement(name);
		anElement.appendChild(field);
		return field;
	}

	/**
	 * 为指定子节点赋值，如果不存在，则创建
	 *
	 * @param ele
	 * @param name
	 * @param value
	 */
	public static void setFieldValue(Element ele, String name, Object value) {
		if ((name == null) || (value == null)) {
			return;
		}
		else {
			Element e = subElement(ele, name);
			if (e == null) {
				e = createField(ele, name);
			}
			setElementValue(e, value);
		}
	}

	/**
	 * 取得Attr的值
	 *
	 * @param ele
	 * @param name
	 *            属性名称
	 *
	 * @return 属性值
	 */
	public static String getAttrValue(Element ele, String name) {
		if (name == null) {
			return null;
		}
		else {
			return ele.getAttribute(name);
		}
	}

	/**
	 * return the text string of the given XPath's node
	 *
	 * @param Node
	 *            node
	 * @param xql
	 *            XPath string
	 * @return the node's value
	 */
	public static String getNodeValue(Node node, String xql) throws XmlUtilException {
		Node childNode = findNode(node, xql);
		if (childNode == null) {
			return null;
		}
		else {
			return getNodeValue(childNode);
		}
	}

	/**
	 * find NodeIterator from node use by the given XPath
	 *
	 * @param node a node
	 * @param xql XPath
	 * @return NodeIterator
	 */
	public final static NodeIterator findNodeIterator(Node node, String xql) throws XmlUtilException {
		return findNodeIterator(node, xql, false);
	}

	/**
	 * find NodeIterator from node use by the given XPath
	 *
	 * @param node a node
	 * @param xql XPath
	 * @return NodeIterator
	 */
	protected final static NodeIterator findNodeIterator(Node node, String xql, boolean isUseXalanParser) throws XmlUtilException {
		if ((xql == null) || (xql.length() == 0)) {
			throw new XmlUtilException("xpath is null!");
		}

		if (node == null) {
			throw new XmlUtilException("node is null!");
		}
		if (node.getNodeType() != Node.DOCUMENT_NODE) {
			xql = processXql(xql);
		}
		try {
			return XPathAPI.selectNodeIterator(node, xql);

		} catch (Exception e) {
			throw new XmlUtilException(e);
		}
	}

	/**
	 * set the xql node's value, create the node if it doesn't exist
	 *
	 * example: node: <Customer> <name>primeton22</name> </Customer>
	 *
	 * xql: Customer/name value: primeton result: <name>primeton</name>
	 *
	 * @param node basic node
	 * @param xql xPath string
	 * @param value the node's value
	 * @return the given XPath's node
	 */
	public final static Node setNodeValue(Node node, String xql, String value) throws XmlUtilException {
		Node targetNode = findNode(node, xql);
		if (targetNode == null) {
			targetNode = appendNode(node, xql);
		}
		setNodeValue(targetNode, value);
		return targetNode;
	}

	/**
	 * set the xql node's value, create the node if it doesn't exist similiar to
	 * setNodeValue(Node,String,String)
	 *
	 * @param doc the document object
	 * @param xql the XPath
	 * @param value the node's value
	 * @return the given XPath's node
	 */
	public final static Node setNodeValue(Document doc, String xql, String value) throws XmlUtilException {
		Node targetNode = findNode(doc, xql);
		if (targetNode == null) {
			targetNode = appendNode(doc, xql);
		}

		NodeList children = targetNode.getChildNodes();
		int index = 0;
		int length = children.getLength();

		Node tmpNode = null;
		// Remove all of the current contents(TextNode)
		for (index = 0; index < length; index++) {
			tmpNode = children.item(index);
			if (tmpNode.getNodeType() == Node.TEXT_NODE) {
				targetNode.removeChild(tmpNode);
			}
		}

		setNodeValue(targetNode, value);
		return targetNode;
	}

	/**
	 * if 'node' is Element node,then create a text node(base on value
	 * parameter) and append to 'node'. if 'node' is text node,then replace
	 * node's with value parameter
	 *
	 * @param node basic
	 * @param value
	 * example: node:
	 */
	public static void setNodeValue(Node node, String value) {
		if (node == null) {
			return;
		}
		else {
			Node childNode = null;
			switch (node.getNodeType()) {
				case (Node.ELEMENT_NODE):
					childNode = node.getFirstChild();
					if (childNode == null) {
						childNode = node.getOwnerDocument().createTextNode(value);
						node.appendChild(childNode);
					}
					else if (childNode.getNodeType() == Node.TEXT_NODE) {
						childNode.setNodeValue(value);
					}
					else {
						node.appendChild(node.getOwnerDocument().createTextNode(value));
					}
					return;
				case (Node.TEXT_NODE):
					node.setNodeValue(value);
					return;
				case (Node.ATTRIBUTE_NODE):
					node.setNodeValue(value);
					return;
			}
		}
	}

	/**
	 * set a node's attribute value
	 *
	 * @param node
	 * @param xql the node's XPath
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return the target node
	 */
	public final static Node setAttribute(Node node, String xql, String name, String value) throws XmlUtilException {
		Node childNode = findNode(node, xql);

		if (childNode == null) {
			childNode = appendNode(node, xql);
		}

		((Element) childNode).setAttribute(name, value);
		return childNode;
	}

	 /**
    * 找到父节点下具有特定属性具有特定值的一个子节点。
    *
    * @param parent
    * @param attributeName
    * @param value
    * @return
    */
   public static Node getNodeByAttribute(Node parent,String attributeName,String value){
       if(parent == null || attributeName == null || value == null){
           return null;
       }
       NodeList children = parent.getChildNodes();
       for (int i = 0; i < children.getLength(); i++) {
           Node child = children.item(i);
           if(child.hasAttributes() && value.equals(getAttributeValue(child, attributeName))){
               return child;
           }
           Node node = getNodeByAttribute(child, attributeName, value);
           if(node != null){
               return node;
           }
       }
       return null;
   }

	/**
	 * set a node's attribute value
	 *
	 * @param node
	 * @param xql the node's XPath
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return the target node
	 */
	public final static String getAttribute(Node node, String xql, String name) throws XmlUtilException {
		Node childNode = findNode(node, xql);

		if (childNode == null) {
			return null;
		}
		return ((Element) childNode).getAttribute(name);
	}

	/**
	 * 查找节点中指定属性的值
	 * @param node
	 * @param attrName
	 * @return
	 */
	public final static String findAttr(Node node, String attrName) throws XmlUtilException{
		if ((attrName == null) || (attrName.length() == 0)) {
			throw new XmlUtilException("attrName == null");
		}

		if (node == null) {
			throw new XmlUtilException("Source node == null");
		}

		Node attrNode = findNode(node, "@"+attrName);
		if(attrNode != null){
			return attrNode.getNodeValue();
		}

		return null;
	}

	/**
	 * 描述：构建新节点。
	 *
	 * @param path
	 *            新元素名称+属性；规则为：
	 *            ElementName[@AttrName1="AttrVal1"][@AttrName2="AttrVal2"]...
	 *            即xPath的路径规则
	 */
	protected static Element createNode(Document doc, String path) {
		StringTokenizer stk = new StringTokenizer(path, XPATH_ATTR_LINCL.concat(XPATH_ATTR_RINCL).concat(XPATH_ATTR_DELIM).concat(XPATH_ATTR_QUOTA).concat(XPATH_ATTR_SQUOTA));

		String eleName = null;
		String eleAttrName = null;
		String eleAttrVal = null;

		if (stk.hasMoreTokens()) {
			eleName = stk.nextToken();
		}

		if (eleName == null) {
			return null;
		}
		Element retElement = doc.createElement(eleName);

		while (stk.hasMoreTokens()) {

			eleAttrName = stk.nextToken();
			int index = eleAttrName.lastIndexOf(XPATH_ATTR_EQ);
			if (index >= 0) {
				eleAttrName = eleAttrName.substring(0, index);
			}
			if (stk.hasMoreTokens()) {
				eleAttrVal = stk.nextToken();
			}
			if (eleAttrName != null) {
				retElement.setAttribute(eleAttrName, eleAttrVal);
			}
		}

		return retElement;
	}

	/**
	 * append a node use the given XPath
	 *
	 * @param node
	 *            a document object
	 * @param xql
	 *            the XPath
	 * @return the result node
	 * @throws EOSRuntimeException
	 *             <ul>
	 *             <li>异常编号：10117 030 异常类型：运行期 抛出条件：node或者xql为null
	 *             <li>异常编号：10117 040 异常类型：运行期 抛出条件：xql格式错误
	 *             </ul>
	 */
	public final static Node appendNode(Node node, String xql) throws XmlUtilException {

		if ((xql == null) || (xql.length() == 0)) {
			throw new XmlUtilException("xpath is null!");
		}

		if (node == null) {
			throw new XmlUtilException("node is null!");
		}
		String[] saXql = StringUtil.split(xql, XPATH_NODE_DELIM);
		if (saXql.length < 1) {
			throw new XmlUtilException("xpath is invalid!");
		}

		Document doc = getOwnerDocument(node);

		// 判断根结点, 如果 node 不是 Document, 得到他本身
		// 如果是 Document, 可能得到他本身, 也可能是 getDocumentElement()
		// 例如在 Document <root/>下 append 新节点
		// 如果是 xpath = root/name , 结果是 <root><name/></root>
		// 这里的 root 就是 Document
		// 如果是 xpath = root , 结果是 <root><root/></root>
		// 这里的 root 就是 Document.getDocumentElement()
		Node root = getRootNode(node, xql);
		if (root == null) {
			throw new XmlUtilException("root node is null!");
		}

		Node item = root;
		for (int i = 0; i < saXql.length; i++) {
			String itemXql = subXql(saXql, i);
			item = findNode(root, itemXql);

			if ((item == null) || (i == saXql.length - 1)) {
				// item = doc.createElement(saXql[i]);
				Node parentNode = (i == 0 ? null : findNode(root, subXql(saXql, i - 1)));
				if (saXql[i].startsWith(XPATH_ATTR_DELIM)) { // 如果是/root/data/list/@name这样的xpath，创建name属性
					String attrName = saXql[i].substring(1, saXql[i].length());
					if (parentNode == null) {
						((Element) root).setAttribute(attrName, "");
						item = ((Element) root).getAttributeNode(attrName);
					}
					else {
						((Element) parentNode).setAttribute(attrName, "");
						item = ((Element) parentNode).getAttributeNode(attrName);
					}
				}
				else {
					item = createNode(doc, saXql[i]);
					if (parentNode == null) {
						root.appendChild(item);
					}
					else {
						parentNode.appendChild(item);
					}
				}
			}
		}
		return item;
	}

	/**
	 * xpath
	 *
	 * @param saXql
	 * @param index
	 * @return
	 */
	private final static String subXql(String[] saXql, int index) {
		String subXql = "";
		for (int i = 0; i <= index; i++) {
			if (i >= saXql.length) {
				break;
			}
			if (subXql != "") {
				subXql = subXql.concat(XPATH_NODE_DELIM).concat(saXql[i]);
			}
			else {
				subXql = subXql.concat(saXql[i]);
			}
		}
		return subXql;
	}

	// 根据Node的类型, 判断 append node 时, root 节点的位置
	// 如果 node 不是 Document, return 本身
	// 如果 node 是 Document, 需要判断:
	// 1、String[]xpath 的第一位是否和 Document的根结点名称相等,
	// 如果名称不相等, 说明准备在根结点下append, return getDocumentElement();
	// 2、如果相等, 判断String[]xpath的长度,
	// 如果长度 == 1, 说明要在根结点下创建新节点, return getDocumentElement();
	// 如果长度 == 1, 说明创建新节点不在根结点下, return Document
	// 举例说明：在 Document <root/>下 append 新节点
	// 如果是 xpath = root/name , 结果是 <root><name/></root>
	// 如果是 xpath = root , 结果是 <root><root/></root>
	private static Node getRootNode(Node node, String xql) throws XmlUtilException {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			String subXPath = xql;
			if (subXPath.startsWith(".".concat(XPATH_NODE_DELIM))) {
				subXPath = subXPath.substring(2);
			}
			else if (subXPath.startsWith(XPATH_NODE_DELIM)) {
				subXPath = subXPath.substring(1);
			}

			String[] xpath = StringUtil.split(subXPath, XPATH_NODE_DELIM);
			if (xpath.length <= 1) {
				throw new XmlUtilException("xpath is invalid!");
			}
			subXPath = xpath[0];

			Element docElement = getOwnerDocument(node).getDocumentElement();
			String nodeName = docElement.getNodeName();
			if (nodeName.equals(subXPath)) {
				return node;
			}
			else {
				throw new XmlUtilException("xpath is invalid!");
			}
		}
		else {
			return node;
		}
	}

	/**
	 * append a child Node to parent Node
	 *
	 * @param node
	 * @param childNode
	 * @return
	 */
	public final static Node appendNode(Node parentNode, Node childNode) {
		Document doc = parentNode.getOwnerDocument();
		if (doc == childNode.getOwnerDocument()) {
			return parentNode.appendChild(childNode);
		}
		else {
			return parentNode.appendChild(doc.importNode(childNode, true));
		}
	}

	/**
	 * Clone a Node
	 *
	 * @param node
	 * @param isDepth
	 * @return the new node
	 */
	public final static Node cloneNode(Node node, boolean isDepth) {
		return node.cloneNode(isDepth);
	}

	/**
	 * copy a node
	 *
	 * @param node
	 * @param srcXql the source XPath
	 * @param desXql the destination node's parent XPath
	 * @return Node the copied node, return null if the source node doesn't exist
	 */
	public final static Node copyNode(Node node, String srcXql, String desXql) throws XmlUtilException {
		Node srcNode = findNode(node, srcXql);
		if (srcNode == null) {
			return null;
		}
		Node targetNode = findNode(node, desXql);
		if (targetNode == null) {
			targetNode = appendNode(node, desXql);
		}
		else {
			if (targetNode.getNodeType() == Node.DOCUMENT_NODE) {
				targetNode = getOwnerDocument(targetNode).getDocumentElement();
			}
		}

		if (targetNode.getNodeType() != Node.ELEMENT_NODE) {
			throw new XmlUtilException("destination node's type invalid, expected is ELEMENT_NODE, actual is " + targetNode.getNodeType());
		}

		Node resultNode = cloneNode(srcNode, true);
		if (resultNode.getNodeType() == Node.ATTRIBUTE_NODE) {
			((Element) targetNode).setAttribute(resultNode.getNodeName(), getNodeValue(resultNode));
		}
		else {
			appendNode(targetNode, resultNode);
		}

		return resultNode;
	}

	/**
	 * move a node if the target node is not exist, create a new node by desXql
	 *
	 * @param node
	 * @param srcXql the source XPath
	 * @param desXql the destination node's parent XPath
	 * @return Node the moved node, return null if the source node doesn't exist
	 */
	public final static Node moveNode(Node node, String srcXql, String desXql) throws XmlUtilException {
		Node childNode = findNode(node, srcXql);
		if (childNode == null) {
			return null;
		}
		Node targetNode = findNode(node, desXql);
		if (targetNode == null) {
			targetNode = appendNode(node, desXql);
		}

		childNode = cloneNode(childNode, true);
		removeNode(node, srcXql);
		targetNode.appendChild(childNode);
		return targetNode;
	}

	/**
	 * move some node to target if the target node is not exist, create a new
	 * node by desXql
	 *
	 * @param node
	 * @param srcXql the source XPath
	 * @param desXql the destination node's parent XPath
	 * @return the moved node's count
	 */
	public final static int moveNodes(Node node, String srcXql, String desXql) throws XmlUtilException {
		Node targetNode = findNode(node, desXql);
		if (targetNode == null) {
			targetNode = appendNode(node, desXql);
		}

		NodeList nodes = findNodes(node, srcXql);
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Node tmp = nodes.item(i);
			Node newNode = tmp.cloneNode(true);
			targetNode.appendChild(newNode);
		}
		removeNodes(node, srcXql);// added by at 2005-10-08
		return length;
	}

	/**
	 * remove a node that the given XPath from a document
	 *
	 * @param node
	 * @param xql
	 * @return
	 * @throws EOSRuntimeException
	 *             参照{@link #findNode(Node, String)}}
	 */
	public final static Node removeNode(Node node, String xql) throws XmlUtilException {
		Node childNode = findNode(node, xql);
		if (childNode == null) {
			return null;
		}
		return childNode.getParentNode().removeChild(childNode);
	}

	/**
	 * remove a node list that the given XPath from a document
	 *
	 * @param node
	 * @param xql
	 */
	public final static void removeNodes(Node node, String xql) throws XmlUtilException {
		NodeList nodes = findNodes(node, xql);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node tmp = nodes.item(i);
			tmp.getParentNode().removeChild(tmp);
		}
	}

	/**
	 * 删掉仅仅由 空格 回车 结束符 等构成的TextNode
	 *
	 * @param node
	 *            节点
	 *
	 * @return 干净的TextNode
	 */
	public static Node removeSpaceNode(Node node) {
		NodeList list = node.getChildNodes();

		if ((list.getLength() == 1) && (list.item(0).getNodeType() == Node.TEXT_NODE) && (node.getNodeType() == Node.ELEMENT_NODE)) {
			return node;
		}

		for (int i = 0; i < list.getLength(); i++) {
			Node tmpNode = list.item(i);
			if (tmpNode.getNodeType() == Node.TEXT_NODE) {
				String str = tmpNode.getNodeValue();
				if (isSpace(str)) {
					tmpNode.getParentNode().removeChild(tmpNode);
					i--;
				}
			}
			else if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				removeSpaceNode(tmpNode);
			}
		}
		return node;
	}

	/**
	 * 除掉所有子节点, 准备重新赋值
	 *
	 * @param node
	 */
	public static void removeAllChildNode(Node node) {
		if (node == null) {
			return;
		}
		Node child = node.getFirstChild();
		while (true) {
			if (child == null) {
				break;
			}
			node.removeChild(child);
			child = node.getFirstChild();
		}
	}

	/**
	 * 在指定节点下创建子节点
	 * @param doc
	 * @param parentNode 要挂靠的父节点
	 * @param nodeName   节点名称
	 * @return 			 新创建的子节点
	 */
	public static Node createElement(Document doc, Node parentNode, String nodeName) {
		Node newNode = doc.createElement(nodeName);
		parentNode.appendChild(newNode);
		return newNode;
	}

	/** 给指定父节点创建子节点(父节点是Document的情况)
	 *
	 * @param Document parent
	 * @param String name
	 * @return Element
	 */
	public static Element createElement(Document document, String name) {
		Element element = document.createElement(name);
		document.appendChild(element);
		return element;
	}

	/** 给指定父节点创建子节点(父节点是Element的情况)
	 *
	 * @param Element parent
	 * @param String name
	 * @return Element
	 */
	public static Element createElement(Element parent, String name) {
		return createField(parent, name, null);
	}

	/** 给指定父节点创建子节点并赋值
	 *
	 * @param NodeElement parent
	 * @param String name
	 * @param String value
	 * @return Element
	 */
	public static Element createField(Element parent, String name, String value) {
		Document document = parent.getOwnerDocument();
		if (document == null) {
			return null;
		}
		//Creates an element of the type specified
		Element element = document.createElement(name);
		//Adds the node newChild to the end of the list of children of this node
		parent.appendChild(element);

		if ((value != null) && !value.equals("")) {
			//Creates a Text node given the specified string
			Text text = document.createTextNode(value);
			element.appendChild(text);
		}
		return element;
	}

	/** 取得指定元素的指定节点的值
	 *
	 * @param Element element
	 * @param String name
	 * @return String
	 */
	public static String getFieldValue(Element element, String name) {
		if ((element == null) || (name == null) || name.equals("")) {
			return null;
		}

		//get the NodeList that contains all children of this node
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node field = children.item(i);
			if ((field instanceof Element) && field.getNodeName().equals(name)) {
				return getTextValue(field);
			}
		}
		return null;
	}

	/** 取得指定子元素
	 *
	 * @param Element element
	 * @param String fieldName
	 * @return Element
	 */
	public static Element getChild(Element element, String fieldName) {
		if ((element == null) || (fieldName == null) || fieldName.equals("")) {
			return null;
		}
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ((child instanceof Element) && child.getNodeName().equals(fieldName)) {
				return (Element) child;
			}
		}
		return null;
	}

	/** 取得节点的TextValue
	 *
	 * @param NodeElement node
	 * @return String
	 */
	public static String getTextValue(Node node) {
		NodeList children = node.getChildNodes();
		if (children == null) {
			return ""; //$NON-NLS-1$
		}
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				String value = ((org.w3c.dom.Text) child).getNodeValue();
				if (value != null) {
					value = value.trim();
				}
				return value;
			}
		}
		return null;
	}

	/**
	 * 取得节点属性值
	 * @param node
	 * @param name
	 * @return
	 */
	public static String getAttributeValue(Node node, String name) {
		NamedNodeMap map = node.getAttributes();
		Node item = map.getNamedItem(name);
		if (null != item) {
			return item.getNodeValue();
		}
		else {
			return null;
		}
	}

	/**
	 * 设置元素属性
	 *
	 * @param Element element
	 * @param String name
	 * @param String value
	 */
	public static void setAttribute(Element element, String name, String value) {
		if ((element == null) || (value == null) || value.equals("")) {
			return;
		}
		element.setAttribute(name, value.trim());
	}

	/**
	 * 设置Attr的值
	 * @param ele
	 * @param name
	 * @param value
	 */
	public static void setAttrValue(Element ele, String name, Object value) {
		if (value == null) {
			return;
		}
		else {
			ele.setAttribute(name, value.toString());
		}
	}

	/**
	 * find node array from node use by the given XPath
	 * @param node a node
	 * @param xql XPath
	 * @return the node array, if not find return null
	 * @throws XmlUtilException
	 */
	public final static NodeList findNodes(Node node, String xql) throws XmlUtilException {
		return findNodes(node, xql, false);
	}


	/**
	 * find node array from node use by the given XPath
	 * @param node a node
	 * @param xql XPath
	 * @return the node array, if not find return null
	 * @throws XmlUtilException
	 */
	protected final static NodeList findNodes(Node node, String xql, boolean isUseXalanParser) throws XmlUtilException {
		if ((xql == null) || (xql.length() == 0)) {
			throw new XmlUtilException("xql == null");
		}

		if (node == null) {
			throw new XmlUtilException("Node == null");
		}
		try {
			if (node.getNodeType() != Node.DOCUMENT_NODE) {
				xql = processXql(xql);
			}

			return XPathAPI.selectNodeList(node, xql);

		} catch (Exception e) {
			throw new XmlUtilException(e);
		}
	}
	/**
	 * find a single node from node use by the given XPath
	 * @param node a node
	 * @param xql XPath
	 * @return the node, if not find return null
	 * @throws XmlUtilException
	 */
	public final static Node findNode(Node node, String xql) throws XmlUtilException {
		return findNode(node, xql, false);
	}

	/**
	 * find a single node from node use by the given XPath
	 * @param node a node
	 * @param xql XPath
	 * @return the node, if not find return null
	 * @throws XmlUtilException
	 */
	protected final static Node findNode(Node node, String xql, boolean isUseXalanParser) throws XmlUtilException {
		if ((xql == null) || (xql.length() == 0)) {
			throw new XmlUtilException("xql == null");
		}

		if (node == null) {
			throw new XmlUtilException("Source node == null");
		}
		try {
			if (node.getNodeType() != Node.DOCUMENT_NODE) {
				xql = processXql(xql);
			}
			return XPathAPI.selectSingleNode(node, xql);
		} catch (Exception e) {
			throw new XmlUtilException(e);
		}
	}

	/**
	 * 转换XQL。<BR>
	 * @param xql
	 * @return
	 */
	private final static String processXql(String xql) {
		if ((xql.length() > 1) && (xql.charAt(0) == '/')) {
			xql = ".".concat(xql);
		}
		return xql;
	}

	/**
	 * 从Element的下一级中获得指定名称子Element的Value, 如果重名, 得到第一个
	 * @param Element superEle
	 * @param String subName
	 * @return Element
	 */
	public final static String subElementValue(Element superEle, String subName) {
		Element element = subElement(superEle, subName);
		if (element == null) {
			return null;
		}
		else {
			return getElementValue(element);
		}
	}

	/**
	 * 从Element的下一级中获得指定名称的子Element
	 * 如果重名, 得到第一个
	 * @param Element superEle
	 * @param String subName
	 * @return Element
	 */
	public final static Element subElement(Element superEle, String subName) {
		NodeList list = superEle.getChildNodes();
		if (list == null) {
			return null;
		}
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.getNodeName().equals(subName)) {
					return (Element) node;
				}
			}
		}
		return null;
	}

	/**
	 * 从Element的下一级中获得指定名称的子Element 集合
	 * @param Element superEle
	 * @param String subName
	 * @return Element
	 */
	@SuppressWarnings("unchecked")
	public final static Vector subElementList(Element superEle, String subName) {
		Vector v = new Vector();
		NodeList list = superEle.getChildNodes();
		if (list == null) {
			return null;
		}

		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.getNodeName().equals(subName)) {
					v.add(node);
				}
			}
		}
		return v;
	}

	/**
	 * 从Element中获得value
	 * @param Element ele
	 * @return String
	 */
	public static String getElementValue(Element ele) {
		if (ele == null)
			return null;
		else {
			return getNodeValue(ele);
		}
	}

	/**查找子节点
	 *
	 * @param element
	 * @param name
	 * @return
	 */
	public static Element find(Element element, String name) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ((child instanceof Element) && child.getNodeName().equals(name)) {
				return (Element) child;
			}
		}
		return null;
	}

	public static Element findGrandson(Element element, String childName, String grandsonName) {
		Element child = find(element, childName);

		if (child == null) {
			return null;
		}
		return find(child, grandsonName);
	}

	public static String getGrandsonValue(Element element, String childName, String grandsonName) {
		Element grandson = findGrandson(element, childName, grandsonName);
		if (grandson == null) {
			return null;
		}

		return getTextValue(grandson);
	}

	/**
	 * 从Element Node中获得value
	 * @param Node node
	 * @return String
	 */
	public static String getNodeValue(Node node) {
		String value = null;
		if (node == null) {
			return null;
		}
		else {
			switch (node.getNodeType()) {
				case (Node.ELEMENT_NODE):
					StringBuffer contents = new StringBuffer();
					NodeList childNodes = node.getChildNodes();
					int length = childNodes.getLength();
					if (length == 0) {
						return null;
					}
					for (int i = 0; i < length; i++) {
						if (childNodes.item(i).getNodeType() == Node.TEXT_NODE) {
							contents.append(childNodes.item(i).getNodeValue());
						}
					}
					value = contents.toString();
					break;
				case (Node.TEXT_NODE):
					value = node.getNodeValue();
					break;
				case (Node.ATTRIBUTE_NODE):
					value = node.getNodeValue();
					break;
			}
		}
		if (value == null) {
			return null;
		}
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if ((c == '\t') || (c == '\n')) {
				continue;
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * 返回某个文档节点的XPath( 仅包括各级节点名称 )
	 *
	 * @param node
	 * @return
	 */
	public static String getPath(Node node) {
		String path = "";
		Node current = node;
		while ((current != null) && !(current instanceof Document)) {
			path = current.getNodeName() + "/" + path;
			current = current.getParentNode();
		}
		if (path != null) {
			path = path.substring(0, path.lastIndexOf("/"));
		}
		return path;
	}

	/** 删掉仅仅由空格\回车\结束符等构成的TextNode
	 *
	 * @param node
	 * @return Node
	 */
	public static Node removeSpace(Node node) {
		NodeList list = node.getChildNodes();
		if (list == null || list.getLength() == 0) {
			return node;
		}
		if ((list.getLength() == 1) && (list.item(0).getNodeType() == Node.TEXT_NODE) && (node.getNodeType() == Node.ELEMENT_NODE)) {
			Text text = (Text) node.getChildNodes().item(0);
			String data = text.getData();
			if (data != null) {
				text.setData(data.trim());
			}
			return node;
		}

		for (int i = 0; i < list.getLength(); i++) {
			Node tmpNode = list.item(i);
			if (tmpNode.getNodeType() == Node.TEXT_NODE) {
				String str = tmpNode.getNodeValue();
				if (XmlUtil.isSpace(str)) {
					tmpNode.getParentNode().removeChild(tmpNode);
					i--;
				}
			}
			else if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				removeSpace(tmpNode);
			}
		}
		return node;
	}

	/** 判断字符串里是否为空
	 *
	 * @param String str
	 * @return boolean
	 */
	private static boolean isSpace(String str) {
		if (str == null) {
			return true;
		}
		for (int i = 0; i < str.length(); i++) {
			for (int j = 0; j < chars.length; j++) {
				if (!isSpace(str.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}

	/** 判断字符是否是空格
	 *
	 * @param char c
	 * @return boolean
	 */
	private static boolean isSpace(char c) {
		for (int j = 0; j < chars.length; j++) {
			if (chars[j] == c) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从 xml String 解析一个Document Instance
	 *
	 * @param xml xml字符串
	 * @return Document
	 * @throws IOException
	 * @throws SAXException
	 * @throws XmlUtilException
	 */
	public final static Document parseString(String xml) throws XmlUtilException, SAXException, IOException {
		return parseStringThrowsException(xml);
	}

	/**
	 * 从 xml String 解析一个Document Instance, throw Exception
	 *
	 * @param xml
	 *            xml字符串
	 * @return Document
	 * @throws IOException
	 * @throws SAXException
	 * @throws XmlUtilException
	 *             参照{@link #parseStringThrowsException(String, String)}
	 */
	public final static Document parseStringThrowsException(String xml) throws XmlUtilException, SAXException, IOException {
		String header = stringXMLHeader(xml);
		String encoding = xmlEncoding(header);
		return parseStringThrowsException(xml, encoding);
	}

	/**
	 * 从 xml String 解析一个Document Instance, throw Exception
	 *
	 * @param xml xml字符串
	 * @param encoding 编码方式
	 *
	 * @return Document
	 * @throws IOException
	 * @throws SAXException
	 * @throws XmlUtilException
	 */
	public final static Document parseStringThrowsException(String xml, String encoding) throws XmlUtilException, SAXException, IOException {
		try {
			ByteArrayInputStream bais = null;
			if (encoding == null) {
				bais = new ByteArrayInputStream(xml.getBytes());
			} else {
				bais = new ByteArrayInputStream(xml.getBytes(encoding));
			}

			return getXMLBuilder().parse(bais);
		} catch (XmlUtilException e) {
			throw new XmlUtilException(xml, e);
		} catch (SAXException e) {
			throw new SAXException(xml, e);
		} catch (IOException e) {
			throw new IOException(StringUtil.concat(e.getMessage(), ": ", xml));
		}
	}

	/** 从 xml file 中解析一个Document Instance
	 *
	 * @param  String fileName
	 * @return Document
	 * @throws XmlUtilException
	 */
	public final static Document parseFile(String fileName) throws XmlUtilException {
		return parseFileThrowsException(new File(fileName));
	}

	/**
	 * 从 xml file 中解析一个Document Instance, throw Exception
	 *
	 * @param fileName
	 * @param encoding
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws XmlUtilException
	 */
	public final static Document parseFileThrowsException(String fileName, String encoding) throws XmlUtilException, SAXException, IOException {

		File f = new File(fileName);
		InputStream input = null;
		InputStreamReader isr = null;
		LineNumberReader lnr = null;
		try {
			input = new FileInputStream(f);
			if ((encoding == null) || encoding.equals("")) {
				encoding = System.getProperty("encoding", UTF_XML_ENCODING);
			}

			isr = new InputStreamReader(input, encoding);
			lnr = new LineNumberReader(isr);
			InputSource isrc = new InputSource(lnr);

			Document doc = getXMLBuilder().parse(isrc);
			return doc;
		} catch (XmlUtilException e) {
			throw new XmlUtilException(f.getAbsolutePath(), e);
		} catch (SAXException e) {
			throw new SAXException(f.getAbsolutePath(), e);
		} catch (IOException e) {
			throw new IOException(StringUtil.concat(e.getMessage(), ": ", f.getAbsolutePath()));
		} finally {
			IOUtils.closeQuietly(lnr);
			IOUtils.closeQuietly(isr);
			IOUtils.closeQuietly(input);
		}
	}

	/**
	 * 从 xml file 中解析一个Document Instance, throw Exception
	 * @param file
	 * @return
	 * @throws XmlUtilException
	 */
	public final static Document parseFileThrowsException(File file) throws XmlUtilException {
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			Document document = getXMLBuilder().parse(is);
			return document;
		} catch (Exception e) {
			throw new XmlUtilException(file == null ? "file is null" : file.getAbsolutePath(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * 从 xml file 中解析一个Document Instance, throw Exception
	 * @param file
	 * @return
	 * @throws XmlUtilException
	 */
	public final static Document parseFileThrowsException(File file, String systemId) throws XmlUtilException {

		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			Document document = getXMLBuilder().parse(is, systemId);
			return document;
		} catch (Exception e) {
			throw new XmlUtilException(file == null ? "file is null" : file.getAbsolutePath(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}


	/**
	 * 从 xml Stream 解析一个Document Instance
	 * @param  InputStream stream
	 * @return Document
	 * @throws XmlUtilException
	 */
	public final static Document parseStream(InputStream stream) throws XmlUtilException {
		try {
			return getXMLBuilder().parse(stream);
		} catch (Exception e) {
			throw new XmlUtilException(e);
		}
	}

	/** 从 xml file 中解析一个Document Instance, throw Exception
	 *
	 * @param fileName
	 * @return Document
	 * @throws Exception
	 */
	public final static Document parseStrings(String source) throws Exception {
		return parseStrings(source, UTF_XML_ENCODING);
	}

	/** 从 xml file 中解析一个Document Instance, throw Exception。<BR>
	 * 可以指定字符串的编码类型。<BR>
	 *
	 * @param fileName
	 * @return Document
	 * @throws Exception
	 */
	public final static Document parseStrings(String source, String encoding) throws Exception {
		try {
			if ((source != null) && (source.length() > 1) && (source.charAt(0) == 65279)) {
				source = source.substring(1);
			}

			ByteArrayInputStream inputStream = null;
			source.charAt(0);
			inputStream = new ByteArrayInputStream(source.getBytes(encoding));
			Document document = getXMLBuilder().parse(inputStream);
			return document;
		} catch (Exception e) {
			throw new Exception(source, e);
		}
	}

	/**
	 * convert Document to string
	 *
	 * @param doc a document that wanted to covert.
	 * @return the result string
	 *
	 */
	public final static String doc2String(Document doc) {
		return node2String(doc.getDocumentElement());
	}

	/**
	 * 将XML节点序列化成字符串,使用自定义的xml头信息。
	 *
	 * @param node                待序列化成字符串的节点
	 * @param isFormat            是否采用格式化
	 * @param head                自定义头部信息
	 * @return
	 * @throws XmlUtilException
	 */
	public final static String node2String(Node node, boolean isFormat, String head) throws XmlUtilException {
		String content = node2String(node, isFormat, false);
		if (head == null || head.trim().length() == 0)
			return content;
		else
			return head.concat(content);
	}

	/**
	 * convert the node to string, 默认不进行格式化
	 * @param node a node
	 * @return the result string
	 * @throws XmlUtilException
	 */
	public final static String node2String(Node node) throws XmlUtilException {
		return node2String(node, false);
	}

	/**
	 * convert the node to string<br>
	 * 格式化输出将改变node的结构
	 * @param node a node
	 * @param isFormat true:格式化输出
	 * @return the result string
	 * @throws XmlUtilException
	 */
	public final static String node2String(Node node, boolean isFormat) throws XmlUtilException {
		return node2String(node, isFormat, true);
	}

	/** 将XML节点序列化成字符串
	 *
	 * @param node
	 * @param isFormat
	 * @param hasHead
	 * @return
	 * @throws XmlUtilException
	 */
	public final static String node2String(Node node, boolean isFormat, boolean hasHead) throws XmlUtilException {
		return node2String(node, isFormat, hasHead, UTF_XML_ENCODING);
	}

	/** 将XML节点序列化成字符串
	 *
	 * @param node
	 * @param isFormat
	 * @param hasHead
	 * @return
	 * @throws XmlUtilException
	 */
	public final static String node2String(Node node, boolean isFormat, boolean hasHead, String encoding) throws XmlUtilException {
		if (node == null) {
			return null;
		}

		//The node is a Document
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			Node elem = ((Document) node).getDocumentElement();
			if (elem != null) {
				node = elem;
			}
		}

		CharArrayWriter writer = new CharArrayWriter();

		Properties prop = OutputPropertiesFactory.getDefaultMethodProperties("xml");
		prop.setProperty("encoding", encoding);

		Serializer serializer = SerializerFactory.getSerializer(prop);

		if (isFormat) {
			prop.setProperty(OutputKeys.INDENT, "yes");
			prop.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, TAB_SPACE);
			XmlUtil.removeSpace(node);
		}
		else {
			prop.setProperty(OutputKeys.INDENT, "no");
		}

		serializer.setOutputFormat(prop);
		serializer.setWriter(writer);

		//串行化输出结果
		try {
			serializer.asDOMSerializer().serialize(node);
		} catch (IOException e) {
			throw new XmlUtilException(e);
		}

		String str = new String(writer.toCharArray());
		if (hasHead) {
			if (!str.startsWith("<?xml")) {
				str = ("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n").concat(str);
			}
			return str;
		}
		else {
			if (!str.startsWith("<?xml")) {
				return str;
			}
			int op = str.indexOf("?>");
			str = str.substring(op + "?>".length()).trim();
			if (str.startsWith("\n")) {
				str = str.substring(1);
			}

			return str;
		}
	}

	/**
	 * 将 Document 转化为 String, 保存到指定路径的文件中<br>
	 * 如果文件不存在, 将新建文件, 如果存在, 将覆盖原文件
	 *
	 * @param doc
	 * @param filePath
	 * @throws IOException
	 */
	public static synchronized void saveToFile(Document doc, String filePath) throws XmlUtilException, IOException {
		saveDocument(doc, filePath, false);
	}

	/**
	 * 将 Document 转化为 String, 保存到指定路径的文件中<br>
	 * 如果文件不存在, 将新建文件, 如果存在, 将覆盖原文件
	 *
	 * @param doc
	 * @param file
	 * @throws IOException
	 */
	public static synchronized void saveToFile(Document doc, File file) throws XmlUtilException, IOException {
		saveDocument(doc, file, false);
	}

	/**
	 * 将 Document 转化为 String, 保存到指定路径的文件中<br>
	 * 如果文件不存在, 将新建文件, 如果存在, 将覆盖原文件
	 * @param doc
	 * @param filePath
	 * @param isFormat  是否将 document 进行格式化
	 * @throws IOException
	 */
	public static void saveDocument(Document doc, String filePath, boolean isFormat) throws IOException {
		if ((doc == null) || (filePath == null)) {
			return;
		}
		filePath = filePath.replace('/', File.separatorChar);
		filePath = filePath.replace('\\', File.separatorChar);

		//避免在win2000中, 如果以前存在一个同名但大小写不一致的文件
		//创建出来的文件仍使用原来的文件名
		File file = new File(filePath);

		saveDocument(doc, file, isFormat);
	}

	/**
	 * 将 Document 转化为 String, 保存到指定路径的文件中<br>
	 * 如果文件不存在, 将新建文件, 如果存在, 将覆盖原文件
	 * @param doc
	 * @param filePath
	 * @param isFormat  是否将 document 进行格式化
	 * @throws IOException
	 */
	public static void saveDocument(Document doc, File file, boolean isFormat) throws IOException {
		saveDocument(doc, file, isFormat, XmlUtil.UTF_XML_ENCODING);
	}

	/**
	 * 将 Document 转化为 String, 保存到指定路径的文件中<br>
	 * 如果文件不存在, 将新建文件, 如果存在, 将覆盖原文件
	 * @param doc
	 * @param filePath
	 * @param isFormat  是否将 document 进行格式化
	 * @param encoding 指定流的编码方式
	 * @throws IOException
	 */
	public static void saveDocument(Document doc, File file, boolean isFormat, String encoding) throws IOException {
		if ((doc == null) || (file == null)) {
			return;
		}

		String content = null;
		content = node2String(doc, isFormat, true);

		//避免在win2000中, 如果以前存在一个同名但大小写不一致的文件
		//创建出来的文件仍使用原来的文件名

		if (!file.exists()) {
			file.createNewFile();
			file.renameTo(file);
		}

		BufferedReader in = new BufferedReader(new CharArrayReader(content.toCharArray()));
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));

			String tmp = in.readLine();
			while (true) {
				if (tmp == null) {
					break;
				}
				else {
					writer.write(tmp);
					writer.write("\r\n"); //$NON-NLS-1$
					writer.flush();
				}
				tmp = in.readLine();
			}
		} catch (IOException e) {
			throw new IOException(StringUtil.concat(e.getMessage(), ": ", file.getAbsolutePath()));
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	static {
		dbf.setIgnoringElementContentWhitespace(true);
	}


	/** 实例化一个DocumentBuilder
	 *
	 * @return DocumentBuilder
	 * @throws XmlUtilException
	 */
	private final static DocumentBuilder getXMLBuilder() throws XmlUtilException {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db;
		} catch (ParserConfigurationException e) {
			throw new XmlUtilException(e);
		}
	}

	/** 以指定的charset读文件
	 *
	 * @param file  文件名
	 * @param charsetName 指定的charset
	 * @return  文件内容
	 * @throws Exception
	 */
	public static String readFile(File file, String charsetName) throws Exception {
		if ((file == null) || !file.exists()) {
			return null;
		}

		FileInputStream inputStream = null;

		try {
			inputStream = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charsetName));
			String line = null;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append("\n"); //$NON-NLS-1$
			}
			return buffer.toString();
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}
