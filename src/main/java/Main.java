import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;


/*
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
*/

public class Main {
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, TransformerException {

        String jatsPath = "files/erudit_all_jats";

        DocFiles file = new DocFiles(jatsPath);
        List<JATSDocument> jatsDocuments = file.getJatsDocuments();
        for (int i = 0; i < jatsDocuments.size(); i++) {
            JATSDocument jatsDocument = jatsDocuments.get(i);
            jatsDocument.writeTeiOutput("output");
        }


        /*
        String teiString = FileUtils.readFileToString(teiFile);
        String txtString = FileUtils.readFileToString(txtFile);

        Matcher matcher = Pattern.compile("[^\\n]{20}\\n[^\\n]{20}").matcher(txtString);

        List<String> matches = Pattern.compile("[^\\n]{20}\\n[^\\n]{20}")
                .matcher(txtString)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList());

        matches.forEach(match->{
            System.out.print(match);
        })*/
    }
}
