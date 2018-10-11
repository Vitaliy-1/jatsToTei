import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocFiles extends File {

    private List<JATSDocument> jatsDocuments;

    public DocFiles(String pathname) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        super(pathname);

        List<JATSDocument> jatsDocuments = new ArrayList<>();
        if (this.isFile()) {
            JATSDocument jatsDocument = new JATSDocument(this);
            if (!jatsDocument.fullTextExists()) {
                jatsDocuments.add(jatsDocument);
            }
        } else if (this.isDirectory()) {
            File[] files = this.listFiles();
            assert files != null;
            for (File file : files) {
                JATSDocument jatsDocument = new JATSDocument(file);
                if (!jatsDocument.fullTextExists()) {
                    jatsDocuments.add(jatsDocument);
                }
            }
        }

        this.jatsDocuments = jatsDocuments;
    }

    public List<JATSDocument> getJatsDocuments() {
        return this.jatsDocuments;
    }
}
