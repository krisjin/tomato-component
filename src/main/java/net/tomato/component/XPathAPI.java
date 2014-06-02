package net.tomato.component;


import java.util.ListIterator;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

/**
 * 
 * @author ScottWang
 */

class EOSNodeList implements NodeList{
    private int cursor;
    private int max=10;
    private Object[] nodes=new Object[10];
    
    public EOSNodeList(){
    }
    
    public int getLength(){
        return cursor;
    }
    
    public Node item(int index){
        return (Node)nodes[index];
    }
    
    public void append(Node node){
        nodes[cursor] = node;
        cursor++;
        if( cursor>=max ) expand();
    }
    
    public void expand(){
        Object[] oldObjs=nodes;
        max += 10;
        nodes = new Object[max];
        System.arraycopy(oldObjs,0,nodes,0,cursor);
    }
}

public class XPathAPI{

    private final static String CURRENT_NODE = ".";
    private static String realXPath(Node contextNode,String xPath){
        if(contextNode.getNodeType() == Node.DOCUMENT_NODE){
            if(xPath.startsWith("/")){
                return xPath;
            }
            Element root = ((Document)contextNode).getDocumentElement();
            int op = xPath.indexOf("/");
            
            //处理这样的查询: Element root = (Element) XMLUtil.findNode(doc, "package");
            if(op == -1){
//            	if (root == null) {
//            		return null;
//            	}
                if(root.getNodeName().equals(xPath)){
                    return CURRENT_NODE;
                }else{
                    return null;
                }
            }
            
            String curXPath = xPath.substring(0, op);
            if(root.getNodeName().equals(curXPath)){
                return xPath.substring(op + 1);
            }else{
                return null;
            }
        }
        return xPath;
    }
    
    /**
     * description: use XPath string to select a single node.
     * @param contextNode	--the node to start searching from.
     * @param xPath			--A valid XPath string.
     * @return the first node found that matches the XPath,or null.
     */
    public static Node selectSingleNode(Node contextNode,String xPath){
        if( contextNode==null || (contextNode.getNodeType()!=Node.DOCUMENT_NODE
        && contextNode.getNodeType()!=Node.ELEMENT_NODE ) ) return null;
        
        
        xPath = XPathAPI.realXPath(contextNode, xPath);
        if(xPath == null){
            return null;
        }
        Node searchNode = realNode(contextNode);
        if(CURRENT_NODE.equals(xPath)){
            return searchNode;
        }
        
        //XPathParser parser = new XPathParser(xPath);//XPathPool.get(xPath);
        XPathParser parser = XPathParser.create(xPath);
        
        EOSNodeList nlResult = new EOSNodeList();
        findNodeList((Element)searchNode,parser,nlResult,true);
        if( nlResult.getLength()>0 ) return nlResult.item(0);
        return null;
    }
    
    
    /**
     * description: use XPath string to select a nodelist.
     * @param	contextNode	--the node to searching from.
     * @param	xPath		--A valid XPath string.
     * @return the nodelist found that matches the XPath,but never be null.
     *		   return value's length is zero means there no matching nodelist.
     */
    public static NodeList selectNodeList(Node contextNode,String xPath){
        if( contextNode==null || (contextNode.getNodeType()!=Node.DOCUMENT_NODE
        && contextNode.getNodeType()!=Node.ELEMENT_NODE ) ) return null;
        //if( !checkNode(contextNode) ) return null;

        xPath = XPathAPI.realXPath(contextNode, xPath);
        if(xPath == null){
            return null;
        }        
        Node searchNode = realNode(contextNode);
        if(CURRENT_NODE.equals(xPath)){
            EOSNodeList nlResult = new EOSNodeList();
            nlResult.append(searchNode);
            return nlResult;
        }
        
        //XPathParser parser = new XPathParser(xPath);//XPathPool.get(xPath);
        XPathParser parser = XPathParser.create(xPath);
        
        EOSNodeList nlResult = new EOSNodeList();
        findNodeList((Element)searchNode,parser,nlResult,false);
        return (NodeList)nlResult;
    }
    
    /**
     * description: use XPath string to select a node iterator.
     * @param	contextNode	--the node to searching from.
     * @param	xPath		--A valid XPath string.
     * @return the nodeiterator found that matches the XPath,but never be null.
     */
    public static NodeIterator selectNodeIterator(Node contextNode,String xPath){
        NodeSet ns = new NodeSet(selectNodeList(contextNode,xPath));
        return (NodeIterator)ns;
    }
    

	private final static void findNodeList(Element searchNode,XPathParser parser,
		EOSNodeList result,boolean single){
		NodeList nl = searchNode.getChildNodes();
		for(int i=0;i<nl.getLength();i++){
            Node curNode = nl.item(i);
            if( curNode==null || curNode.getNodeType()!=Node.ELEMENT_NODE ) continue;
			_findNodeList((Element)curNode,parser.listIterator(),result,single);
        }
	}

    private final static void _findNodeList(Element currNode,ListIterator<?> itXPath,
	    EOSNodeList result,boolean single){

        if( single && result.getLength()>0 ) return;
        if( !itXPath.hasNext() ) return;

        //if first step is attribute(style like '@attribute') and attribute
        //exist in current node,put corresponding attribute into result.
        //(follow codes were special handle for first step.)
        NodeDescription curDesc = (NodeDescription)itXPath.next();
        if( curDesc.getType()==NodeDescription.NODE_ATTR ){
            Node node = currNode.getParentNode();
            if( node==null || node.getNodeType()==Node.DOCUMENT_NODE ){
                Element parent = currNode;
                //if( curDesc.equals(parent) ) result.append(curDesc.m_attr);
                if( curDesc.equals(parent) ) result.append(curDesc.getAttribute(parent));
            }
            return;
        }
        //compare to current node.
        if( !curDesc.equals(currNode) ) return;

        //test next step type,find corresponding attribute
        //in current node if attribute.
        if( itXPath.hasNext() ){
            NodeDescription nextDesc = (NodeDescription)itXPath.next();
            if( itXPath.hasPrevious() ) itXPath.previous();
            if( nextDesc.getType()==NodeDescription.NODE_ATTR ){
                //if( nextDesc.equals(currNode) )	result.append(nextDesc.m_attr);
                if( nextDesc.equals(currNode) )	result.append(nextDesc.getAttribute(currNode));
                return;
            }
        }

        //use same agorithem search in current node's childrens
        if( itXPath.hasNext() ){
            NodeList nlChilds = currNode.getChildNodes();
            int nCount = nlChilds.getLength();

            for( int i=0;i<nCount;i++ ){
                Node currChild = nlChilds.item(i);
                //if( !checkNode(currChild) ){
                if( currChild==null || (currChild.getNodeType()!=Node.DOCUMENT_NODE
                && currChild.getNodeType()!=Node.ELEMENT_NODE ) ){
                    continue;
                }

                _findNodeList((Element)currChild,itXPath,result,single);
                if( itXPath.hasPrevious() ) itXPath.previous();
            }
            return;
        }
        result.append(currNode);
    }   
    
    private static final Node realNode(Node node){
        if( node.getNodeType() == Node.DOCUMENT_NODE )
            return ((Document)node).getDocumentElement();
        else
            return node;
    }
    
}
