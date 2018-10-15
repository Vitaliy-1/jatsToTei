import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocFiles {
	private File jatsFile;
	private File txtFile;

    private List<JATSDocument> jatsDocuments;

    public DocFiles(File jatsFile, File txtFile) {
       this.jatsFile = jatsFile;
       this.txtFile = txtFile;
    }

    public List<JATSDocument> getJatsDocuments() throws ParserConfigurationException, SAXException, IOException {
	    List<JATSDocument> jatsDocuments = new ArrayList<>();
	    if (this.jatsFile.isFile()) {
		    JATSDocument jatsDocument = new JATSDocument(this.jatsFile);
		    jatsDocuments.add(jatsDocument);
	    } else if (this.jatsFile.isDirectory()) {
		    File[] files = this.jatsFile.listFiles();
		    assert files != null;
		    for (File file : files) {
			    JATSDocument jatsDocument = new JATSDocument(file);
			    jatsDocuments.add(jatsDocument);
		    }
	    }
	
	    this.jatsDocuments = jatsDocuments;
	    
    	return this.jatsDocuments;
    }
    
    public Map<JATSDocument, String> getComplementaryFiles() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    	
    	Map<JATSDocument, String> complementaryFiles = new HashMap<>();
	    if (this.jatsFile.isFile() && this.txtFile.isFile()) {
		    JATSDocument jatsDocument = new JATSDocument(this.jatsFile);
		    complementaryFiles.put(jatsDocument, FileUtils.readFileToString(this.txtFile));
	    } else if (this.jatsFile.isDirectory() && this.txtFile.isDirectory()) {
		    File[] jatsFiles = this.jatsFile.listFiles();
		    assert jatsFiles != null;
		    for (File jatsFile : jatsFiles) {
			    String jatsFileName = jatsFile.getName().replace(".jats.xml", "") ;
			    JATSDocument jatsDocument = new JATSDocument(jatsFile);
			    if (jatsDocument.fullTextExists()) {
				    File[] txtFiles = this.txtFile.listFiles();
				    assert txtFiles != null;
				    for (File txtFile : txtFiles) {
					    String txtFileName = txtFile.getName().replaceAll(".pdf.txt", "");
					    if (jatsFileName.equals(txtFileName)) {
						    complementaryFiles.put(jatsDocument, FileUtils.readFileToString(txtFile));
					    }
				    }
			    }
		    }
	    }
	    
	    return  complementaryFiles;
	}
}
