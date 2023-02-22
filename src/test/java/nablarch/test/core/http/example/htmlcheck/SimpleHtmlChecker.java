package nablarch.test.core.http.example.htmlcheck;

import nablarch.core.util.FileUtil;
import nablarch.test.tool.htmlcheck.HtmlChecker;
import nablarch.test.tool.htmlcheck.InvalidHtmlException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class SimpleHtmlChecker implements HtmlChecker {

    private String encoding;
    
    public void checkHtml(File html) throws InvalidHtmlException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = null;
        
        
        try {
            reader = new InputStreamReader(new FileInputStream(html), encoding);

            char[] buf = new char[1024];
            int len = 0;
            while ((len = reader.read(buf)) > 0) {
                sb.append(buf, 0, len);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        
        if (!sb.toString().trim().startsWith("<html>")) {
            throw new InvalidHtmlException("html not starts with <html>");
        }
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
