import org.jetbrains.annotations.NotNull;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class JATSDocument {
	
	private XPath xPath;
	private File file;
	private Document jatsDocument;
	private Document teiDocument;
	private String teiString;
	
	public JATSDocument(File file) throws IOException, SAXException, ParserConfigurationException {
		this.file = file;
		
		// Disable validation
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilderFactory.setValidating(false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(file);
		document.getDocumentElement().normalize();
		this.jatsDocument = document;
		XPathFactory xPathFactory = XPathFactory.newInstance();
		this.xPath = xPathFactory.newXPath();
		
	}
	
	public String getFilePath() {
		return this.file.getPath();
	}
	
	public String getFileName() {
		return this.file.getName();
	}
	
	public Document getTeiFullText() throws XPathExpressionException, ParserConfigurationException {
		Node bodyNode = (Node) this.xPath.compile("/article/body").evaluate(this.jatsDocument, XPathConstants.NODE);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document teiDocument = documentBuilder.newDocument();
		this.teiDocument = teiDocument;
		
		Node rootNode = teiDocument.createElement("tei");
		((Element) rootNode).setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", "http://www.w3.org/1999/xlink");
		teiDocument.appendChild(rootNode);
		
		Node teiHeaderNode = teiDocument.createElement("teiHeader");
		rootNode.appendChild(teiHeaderNode);
		
		Element fileDescNode = teiDocument.createElement("fileDesc");
		fileDescNode.setAttribute("xml:id", "0");
		rootNode.appendChild(fileDescNode);
		
		Node bodyClonedNode = null;
		try {
			bodyClonedNode = bodyNode.cloneNode(true);
		} catch (Exception e) {
			System.out.println("Body tag is missing from input XML in " + this.getFileName());
			e.printStackTrace();
		}
		
		teiDocument.adoptNode(bodyClonedNode);
		rootNode.appendChild(bodyClonedNode);
		
		nodesToTei();
		
		teiDocument.normalize();
		
		return this.teiDocument;
	}
	
	public void writeTeiOutput(String outputDir) throws TransformerException, XPathExpressionException, ParserConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource domSource = new DOMSource(this.getTeiFullText());
		StreamResult streamResult = new StreamResult(new File(outputDir + "/" + this.getFileName().replace(".jats.", ".tei.")));
		transformer.transform(domSource, streamResult);
	}
	
	public String getTeiString() throws TransformerException, XPathExpressionException, ParserConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource domSource = new DOMSource(this.getTeiFullText());
		StringWriter stringWriter = new StringWriter();
		StreamResult streamResult = new StreamResult(stringWriter);
		transformer.transform(domSource, streamResult);
		
		this.teiString = stringWriter.toString();
		
		return this.teiString;
	}
	
	
	private void nodesToTei() throws XPathExpressionException {
		renameNodes("/tei/body", "text");
		renameNodes("//title", "head");
		
		// adding linebreak at the end of the element
		appendLast("//table-wrap/label", "lb");
		appendLast("//table-wrap/caption/title", "lb");
		
		// replacing figure and box label with title
		replaceAsLastChild("//boxed-text", "label");
		replaceAsLastChild("//boxed-text", "caption");
		replaceAsLastChild("//fig", "label");
		replaceAsLastChild("//fig", "caption");
		
		// boxes
		HashMap<String, String> boxAttributes = new HashMap<>();
		boxAttributes.put("type", "box");
		resetAttribute("//boxed-text", "content-type");
		renameNodes("//boxed-text", "figure", boxAttributes);
		
		// lists
		resetAttribute("//list", "list-type");
		removeNodesWithoutChidren("p", "list-item");
		renameNodes("//list-item", "item");
		
		// Tables and figures
		HashMap<String, String> tableAttributes = new HashMap<>();
		tableAttributes.put("type", "table");
		resetAttribute("//table-wrap", "id");
		renameNodes("//table-wrap", "figure", tableAttributes);
		removeNodesWithoutChidren("//caption");
		HashMap<String, String> figureAttributes = new HashMap<>();
		figureAttributes.put("type", "figure");
		resetAttribute("//fig", "id");
		renameNodes("//fig", "figure", figureAttributes);
		
		removeNodesWithoutChidren("//sec");
		removeNodesWithoutChidren("//disp-quote");
		removeNodesWithoutChidren("//italic");
		removeNodesWithoutChidren("//bold");
		removeNodesWithoutChidren("//sup");
		removeNodesWithoutChidren("//sub");
		removeNodesWithoutChidren("//xref"); // these are only references to notes which are not supported by Grobid
		
		removeNodes("graphic");
		removeNodes("label", "text");
		removeAppendPrev("attrib");
		removeNodesWithoutChidren("label", "figure");
		removeNodesWithoutChidren("head", "figure");
		removeNodesWithoutChidren("p", "figure");
		
		// removing empty lines
		NodeList emptyTextNodes = (NodeList) this.xPath.compile("//text()[normalize-space(.) = '']").evaluate(this.teiDocument, XPathConstants.NODESET);
		
		for (int i = 0; i < emptyTextNodes.getLength(); i++) {
			Node emptyTextNode = emptyTextNodes.item(i);
			emptyTextNode.getParentNode().removeChild(emptyTextNode);
		}
	}
	
	// Renaming nodes
	
	private void renameNodes(String xpathExpression, String tagName) throws XPathExpressionException {
		NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				this.teiDocument.renameNode(nodeList.item(i), nodeList.item(i).getNamespaceURI(), tagName);
			}
		}
	}
	
	// Renaming nodes plus setting attribute from associative array arttibute -> value
	
	private void renameNodes(String xpathExpression, String tagName, HashMap<String, String> attributeMap) throws XPathExpressionException {
		NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element) {
				Node node = nodeList.item(i);
				this.teiDocument.renameNode(node, node.getNamespaceURI(), tagName);
				
				for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
					String attribute = entry.getKey();
					String value = entry.getValue();
					((Element) node).setAttribute(attribute, value);
				}
			}
		}
	}
	
	// Removing nodes without children
	
	private void removeNodesWithoutChidren(String xpathExpression) throws XPathExpressionException {
		NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Node parentNode = node.getParentNode();
			Node nextNode = node.getNextSibling();
			if (nodeList.item(i).hasChildNodes()) {
				NodeList childNodes = node.getChildNodes();
				for (int y = 0; y < childNodes.getLength(); y++) {
					if (nextNode != null) {
						parentNode.insertBefore(childNodes.item(y).cloneNode(true), nextNode);
					} else {
						parentNode.appendChild(childNodes.item(y).cloneNode(true));
					}
				}
			}
			
			parentNode.removeChild(node);
			parentNode.normalize();
		}
		// remove nested elements
		NodeList nodeListSecond = (NodeList) xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		if (nodeListSecond.getLength() > 0) {
			removeNodesWithoutChidren(xpathExpression);
		}
	}
	
	private void removeNodesWithoutChidren(String xpathExpression, String parentNodeName) throws XPathExpressionException {
		NodeList nodeListFirst = (NodeList) this.xPath.compile("//" + parentNodeName).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int x = 0; x < nodeListFirst.getLength(); x++) {
			Node nodeFirst = nodeListFirst.item(x);
			NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(nodeFirst, XPathConstants.NODESET);
			
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				Node parentNode = node.getParentNode();
				Node nextNode = node.getNextSibling();
				if (nodeList.item(i).hasChildNodes()) {
					NodeList childNodes = node.getChildNodes();
					for (int y = 0; y < childNodes.getLength(); y++) {
						Node clonedNode = childNodes.item(y).cloneNode(true);
						clonedNode.normalize();
						if (nextNode != null) {
							parentNode.insertBefore(clonedNode, nextNode);
						} else {
							parentNode.appendChild(clonedNode.cloneNode(true));
						}
					}
				}
				
				parentNode.removeChild(node);
				parentNode.normalize();
			}
		}
	}
	
	// Removing node and appending content inside previous element (e.g., for attrib)
	
	private void removeAppendPrev(String tagName) {
		NodeList nodeList = this.teiDocument.getElementsByTagName(tagName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Node parentNode = node.getParentNode();
			Node prevNode = getPreviousSiblingElement(node);
			if (nodeList.item(i).hasChildNodes()) {
				NodeList childNodes = node.getChildNodes();
				for (int y = 0; y < childNodes.getLength(); y++) {
					if (prevNode != null) {
						prevNode.appendChild(this.teiDocument.createTextNode(" "));
						prevNode.appendChild(childNodes.item(y).cloneNode(true));
					} else {
						parentNode.appendChild(childNodes.item(y).cloneNode(true));
					}
				}
			}
			
			parentNode.removeChild(node);
			parentNode.normalize();
		}
		
		NodeList nodeListSecond = this.teiDocument.getElementsByTagName(tagName);
		if (nodeListSecond.getLength() > 0) {
			removeAppendPrev(tagName);
		}
	}
	
	private void removeNodes(String tagName) {
		NodeList nodeList = this.teiDocument.getElementsByTagName(tagName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Node parentNode = node.getParentNode();
			parentNode.removeChild(node);
			parentNode.normalize();
		}
		
		NodeList nodeListSecond = this.teiDocument.getElementsByTagName(tagName);
		if (nodeListSecond.getLength() > 0) {
			removeNodes(tagName);
		}
	}
	
	private void removeNodes (String xpathExpression, String parentNodeName) throws XPathExpressionException {
		NodeList nodeListFirst = (NodeList) this.xPath.compile("//" + parentNodeName).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int x = 0; x < nodeListFirst.getLength(); x++) {
			Node nodeFirst = nodeListFirst.item(x);
			NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(nodeFirst, XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				Node parentNode = node.getParentNode();
				parentNode.removeChild(node);
				parentNode.normalize();
			}
		}
	}
	
	private void resetAttribute(String xpathExpression, String attribute) throws XPathExpressionException {
		NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			node.getAttributes().removeNamedItem(attribute);
		}
	}
	
	private Element getPreviousSiblingElement(@NotNull Node node) {
		Node prevSibling = node.getPreviousSibling();
		while (prevSibling != null) {
			if (prevSibling.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) prevSibling;
			}
			prevSibling = prevSibling.getPreviousSibling();
		}
		
		return null;
	}
	
	private void appendLast(String xpathExpression, String newNodeName) throws XPathExpressionException {
		NodeList tableLabels = (NodeList) this.xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int y = 0; y < tableLabels.getLength(); y++) {
			Element lb = teiDocument.createElement(newNodeName);
			tableLabels.item(y).appendChild(lb);
		}
	}
	
	private void replaceAsLastChild(String xpathExpression, String nodeName) throws XPathExpressionException {
		NodeList nodeList = (NodeList) this.xPath.compile(xpathExpression).evaluate(this.teiDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			
			Node boxedText = nodeList.item(i);
			Node boxedLabel = (Node) this.xPath.compile(nodeName).evaluate(boxedText, XPathConstants.NODE);
			if (boxedLabel != null) {
				Node clonedLabel = boxedLabel.cloneNode(true);
				boxedText.removeChild(boxedLabel);
				
				if (nodeName.equals("label")) {
					Element lb = this.teiDocument.createElement("lb");
					boxedText.appendChild(lb);
				}
				boxedText.appendChild(clonedLabel);
			}
		}
	}
	
	public boolean fullTextExists() throws XPathExpressionException {
		NodeList nodeList = (NodeList) this.xPath.compile("//body/node()").evaluate(this.jatsDocument, XPathConstants.NODESET);
		if (nodeList.getLength() != 0) {
			return true;
		}
		
		System.err.println(this.getFileName() + " has empty body tag");
		return false;
	}
}
