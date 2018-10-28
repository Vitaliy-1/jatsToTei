import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LineBreaker implements Runnable {
	private String tei;
	private String text;
	private String outputDir;
	private String fileName;
	
	public LineBreaker(String tei, String text, String fileName, String outputDir) {
		this.tei = tei;
		this.text = text;
		this.outputDir = outputDir;
		this.fileName = fileName;
	}
	
	@Override
	public void run() {
		String teiDocument = this.tei;
		String textString = this.text;
		Pattern pattern = Pattern.compile("[^\n]{20}\n[^\n]{20}");
		List<String> linebreak_contexts = new ArrayList<>();
		Matcher m = pattern.matcher(textString);
		while (m.find()) {
			linebreak_contexts.add(m.group());
		}
		
		for (String x: linebreak_contexts) {
			String x_lb = x.replace("\n", "<lb/>");
			x = x.replaceAll("\n", " "); // formatting string to match it inside tei file
			
			teiDocument = teiDocument.replaceAll("\\s{2,}", " "); // deleting extra whitespaces (why they are present in original JATS XML in the first place?)
			teiDocument = teiDocument.replace(x, x_lb);
			
			
	    /*
	     * Check strings match for debugging purposes
			System.out.println("x: => " + x);
			System.out.println("x_lb: => " + x_lb);
		 */
		}
		
		teiDocument = teiDocument.replaceAll(">\\s+<", "><");
		
		teiDocument = teiDocument.replaceAll("</head>", "<lb/></head>");
		teiDocument = teiDocument.replaceAll("</item>", "<lb/></item>");
		teiDocument = teiDocument.replaceAll("</row>", "<lb/></row>");
		teiDocument = teiDocument.replaceAll("</p>", "<lb/></p>");
		teiDocument = teiDocument.replaceAll("</figure>", "<lb/></figure>");
		teiDocument = teiDocument.replaceAll("<figure type=\"figure\"><lb/>\\s+", "<figure type=\"figure\">"); //replace redundant linebreaks if needed
		
		teiDocument = parseCitations(teiDocument);
		teiDocument = parseTableRef(teiDocument);
		teiDocument = parseFigureRef(teiDocument);
		
		try {
			stringToDom(teiDocument);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
	}
	
	@NotNull
	private String escapeRegex(String x) {
		x = x.replace("(", "\\(");
		x = x.replace(")", "\\)");
		x = x.replace("[", "\\[");
		x = x.replace("]", "\\]");
		x = x.replace("*", "\\*");
		x = x.replace(".", "\\.");
		x = x.replace("?", "\\?");
		x = x.replace("+", "\\+");
		x = x.replace("}", "\\}");
		x = x.replace("{", "\\{");
		return x;
	}
	
	public void stringToDom(String xmlSource) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		// Parse the given input
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = null;
		try {
			doc = builder.parse(new InputSource(new StringReader(xmlSource)));
		} catch (SAXException e) {
			System.err.println(this.fileName + " => ");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Write the parsed document to an xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		
		StreamResult result =  new StreamResult(new File(this.outputDir + "/" + this.fileName.replace(".jats.xml", ".training.fulltext.tei.xml")));
		transformer.transform(source, result);
	}
	
	private String parseCitations(String tei) {
		Pattern pattern = Pattern.compile("\\([A-Z].*?\\)");
		Matcher matcher = pattern.matcher(tei);
		List<String> xrefContext = new ArrayList<>();
		while (matcher.find()) {
			xrefContext.add(matcher.group());
		}
		
		// Remove duplicates to avoid nested refs
		List<String> duplicatesRemoved = xrefContext.stream().distinct().collect(Collectors.toList());
		
		for(String x: duplicatesRemoved) {
			String x_ref = "<ref type=\"biblio\">" + x + "</ref>";
			Pattern pattern1 = Pattern.compile("\\w,(?:\\s+)?\\d{4}|\\w,(?:\\s+)?<lb/>(?:\\s+)?\\d{4}|\\w\\s+\\d{4}");
			Matcher matcher1 = pattern1.matcher(x);
			if (matcher1.find()) {
				x = escapeRegex(x);
				tei = tei.replaceAll(x, x_ref);
			}
			
		}
		
		return tei;
	}
	
	private String parseTableRef(String tei) {
		Pattern pattern = Pattern.compile(".{30}[Tt]abl\\w{1,3}(\\xa0+|\\s+)\\d{1,2}"); // parsing 30 symbol behind to put some logic
		Matcher matcher = pattern.matcher(tei);
		List<String> xrefContext = new ArrayList<>();
		
		while (matcher.find()) {
			xrefContext.add(matcher.group());
		}
		
		// Remove duplicates to avoid nested refs
		List<String> duplicatesRemoved = xrefContext.stream().distinct().collect(Collectors.toList());
		
		for(String x: duplicatesRemoved) {
			String firstThirty = x.substring(0, 30);
			if (!firstThirty.contains("<figure")) { // do not capture if table label
				String remainer = x.substring(30);
				String x_ref = firstThirty + "<ref type=\"table\">" + remainer + "</ref>";
				tei = tei.replace(x, x_ref);
			}
		}
		
		return tei;

	}
	
	private String parseFigureRef(String tei) {
		Pattern pattern = Pattern.compile(".{30}[Ff]ig{1,4}\\w{1,3}(\\xa0+|\\s+)\\d{1,2}");
		Matcher matcher = pattern.matcher(tei);
		List<String> xrefContext = new ArrayList<>();
		
		while (matcher.find()) {
			xrefContext.add(matcher.group());
		}
		
		// Remove duplicates to avoid nested refs
		List<String> duplicatesRemoved = xrefContext.stream().distinct().collect(Collectors.toList());
		
		for(String x: duplicatesRemoved) {
			String firstThirty = x.substring(0, 30);
			if (!firstThirty.contains("<figure")) { // do not capture if figure label
				String remainer = x.substring(30);
				String x_ref = firstThirty + "<ref type=\"figure\">" + remainer + "</ref>";
				tei = tei.replace(x, x_ref);
			}
		}
		
		return tei;
	}
}
