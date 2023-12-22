package nablarch.test.core.http.dump;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link SimpleReplacer}のテスト */
public class SimpleReplacerTest {

    private SimpleReplacer target;

    @Before
    public void setUp() {
        Pattern pattern = Pattern.compile("\\s*$");   // 行末の空白
        String replacement = "";  // 空文字
        target = new SimpleReplacer(pattern, replacement, "UTF-8", "\r\n");
    }

    /**
     * {@link SimpleReplacer#replace(Reader, Writer)}のテスト
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testReplaceReader() throws IOException {

        String input = "hoge   \r\n fuga   \r\n fumu\r\n";

        Reader reader = new StringReader(input);
        StringWriter writer = new StringWriter();
        target.replace(reader, writer);
        String actual = writer.toString();
        assertEquals("hoge\r\n fuga\r\n fumu\r\n", actual);

    }


        /**
     * {@link SimpleReplacer#replace(InputStream, OutputStream)}のテスト
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testReplaceStream() throws IOException {

        String input = "hoge   \r\n fuga   \r\n fumu\r\n";

        InputStream in = new ByteArrayInputStream(input.getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        target.replace(in, out);

        assertEquals("hoge\r\n fuga\r\n fumu\r\n", new String(out.toByteArray(), "UTF-8"));
    }

    @Test
    public void testReplaceFile() throws IOException {
        String input = "あいう   \r\n えお   \r\n か き\r\n";
        File inFile = File.createTempFile(getClass().getName(), ".txt");
        inFile.deleteOnExit();
        Writer writer = new OutputStreamWriter(new FileOutputStream(inFile), "UTF-8");
        writer.write(input);
        writer.close();

        File outFile = File.createTempFile(getClass().getName(), ".txt");
        outFile.deleteOnExit();

        target.replace(inFile.getPath(), outFile.getPath());

        Reader reader = new InputStreamReader(new FileInputStream(outFile), "UTF-8");
        StringBuilder actual = new StringBuilder();
        for(int c; (c = reader.read()) != -1;) {
            actual.append((char) c);
        }
        assertEquals("あいう\r\n えお\r\n か き\r\n", actual.toString());
        reader.close();
    }

    @Test(expected = FileNotFoundException.class)
    public void testReplaceFileNotFound() throws IOException {
        target.replace("notFound.txt", "dummy.txt");
    }

    @Test
    public void testCloseQuietly() {
        target.closeQuietly(null);
        final boolean[] isClosed = { false };
        target.closeQuietly(new Closeable() {
            public void close() throws IOException {
                isClosed[0] = true;
            }
        });
        assertTrue(isClosed[0]);

        target.closeQuietly(new Closeable() {
            public void close() throws IOException {
                throw new IOException();
            }
        });
    }
}
