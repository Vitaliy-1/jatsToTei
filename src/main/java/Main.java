import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	private static final int NUMBER_OF_THREADS = 8;
	
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, TransformerException {
	
	    ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        String jatsPath = "files/erudit_all_jats";
        String txtPath = "files/erudit_all_text";
        String outputDir = "fulltextModel";
	
	    File directoryName = new File (outputDir);
	    if (!directoryName.exists()) {
		    directoryName.mkdir();
	    }
		
        File jatsFile = new File(jatsPath);
        File txtFile = new File(txtPath);
        
        DocFiles file = new DocFiles(jatsFile, txtFile);
       
        /*
        List<JATSDocument> jatsDocuments = file.getJatsDocuments();
        for (JATSDocument jatsDocument: jatsDocuments) {
        	jatsDocument.writeTeiOutput("output");
        }
        */
        
        Map<JATSDocument, String> complementaryFiles = file.getComplementaryFiles();
        
        for(Map.Entry<JATSDocument, String> entry : complementaryFiles.entrySet()) {
        	if (entry.getKey() != null && entry.getValue() != null) {
			    String teiDocument = entry.getKey().getTeiString();
			    String txtString = entry.getValue();
			    
		        try {
			        LineBreaker lineBreaker = new LineBreaker(teiDocument, txtString, entry.getKey().getFileName(), outputDir);
			        executor.execute(lineBreaker);
		        } catch (Exception e) {
		        	System.err.println("TEI: " + entry.getKey().getFileName() + " =>");
			        e.printStackTrace();
		        }
		        
	        }
		}
		
		executor.shutdown();
    }
}
