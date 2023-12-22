package nablarch.test.core.http.dump;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * {@link RequestDumpAgent}のテスト
 *
 * @author T.Kawasaki
 */
public class RequestDumpAgentTest {

    /** テスト対象 */
    private RequestDumpAgent target = new RequestDumpAgent();

    /**
     * {@link RequestDumpAgent#getTemplateBook(String)}のテスト<br/>
     * <p/>
     * テンプレートとなるブックがクラスパス上のリソースとして存在しない場合に例外が発生すること
     *
     * @throws IOException 予期しない例外
     */
    @Test(expected = IllegalStateException.class)
    public void testResourceNotFound() throws IOException {
        target.getTemplateBook("not-exists");
    }


    /**
     * {@link RequestDumpAgent#escapeAndJoin(Object[], String)}のテスト<br/>
     * <p/>
     * 引数で与えられた配列が指定した区切り文字で結合されること
     */
    @Test
    public void testEscapeAndJoin() {
        assertNull(target.escapeAndJoin(null, ","));   // nullの場合はnull
        assertEquals("", target.escapeAndJoin(new String[0], ","));  // 空の配列の場合は、空文字
        assertEquals("foo,bar", target.escapeAndJoin(new String[]{"foo", "bar"}, ","));  // カンマ区切り
        assertEquals("\\\\foo\\\\,bar\\\\", target.escapeAndJoin(new String[]{ "\\foo\\", "bar\\"}, ","));  // カンマ区切り
    }

    /**
     * {@link RequestDumpAgent#print(String, Map, OutputStream)}のテスト<br/>
     * パラメータがExcelファイルに出力されること
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testPrint() throws IOException {

        // 入力値
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("key1", new String[]{"値１"});
        params.put("key2", new String[]{"値２"});
        params.put("key3", new String[]{"値3-1", "値3-2"});

        // 出力先ファイル
        File outFile = createTempFile();

        OutputStream out = new FileOutputStream(outFile);
        target.print("fuga", params, out);
        out.close();

        // 出力されたファイルの内容確認
        HSSFWorkbook book = open(outFile);
        HSSFSheet sheet = book.getSheet("sheet1");

        // ID行
        HSSFRow idRow = sheet.getRow(3);
        assertEquals("LIST_MAP=", idRow.getCell(0).toString());
        // キー行
        HSSFRow keyRow = sheet.getRow(4);
        assertEquals("key1", keyRow.getCell(0).toString());
        assertEquals("key2", keyRow.getCell(1).toString());
        assertEquals("key3", keyRow.getCell(2).toString());
        // 値行
        HSSFRow valueRow = sheet.getRow(5);
        assertEquals("値１", valueRow.getCell(0).toString());
        assertEquals("値２", valueRow.getCell(1).toString());
        assertEquals("値3-1,値3-2", valueRow.getCell(2).toString());
    }

    @Test
    public void testYenSignEscaped() throws IOException {

        // 入力値
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("key1", new String[]{"00\\1"});
        params.put("key2", new String[]{"00\\\\1"});
        params.put("key3", new String[]{"値3-1", "値3-2"});

        // 出力先ファイル
        File outFile = createTempFile();

        OutputStream out = new FileOutputStream(outFile);
        target.print("fuga", params, out);
        out.close();

        // 出力されたファイルの内容確認
        HSSFWorkbook book = open(outFile);
        HSSFSheet sheet = book.getSheet("sheet1");

        // ID行
        HSSFRow idRow = sheet.getRow(3);
        assertEquals("LIST_MAP=", idRow.getCell(0).toString());

        // キー行
        HSSFRow keyRow = sheet.getRow(4);
        assertEquals("key1", keyRow.getCell(0).toString());
        assertEquals("key2", keyRow.getCell(1).toString());
        assertEquals("key3", keyRow.getCell(2).toString());

        // 値行
        HSSFRow valueRow = sheet.getRow(5);
        assertEquals("00\\\\1", valueRow.getCell(0).toString());
        assertEquals("00\\\\\\\\1", valueRow.getCell(1).toString());
        assertEquals("値3-1,値3-2", valueRow.getCell(2).toString());

    }

    /** エスケープ処理のテストケース。*/
    @Test
    public void testEscape() {
        // 円マーク
        assertThat(target.escape("\\"), is("\\\\"));
        assertThat(target.escape("\\\\"), is("\\\\\\\\"));
        assertThat(target.escape("\\ho\\ge\\"), is("\\\\ho\\\\ge\\\\"));
        assertThat(target.escape("\thoge\n"), is("\thoge\n"));

        // カンマがエスケープされること。
        assertThat(target.escape(","), is("\\,"));
        assertThat(target.escape("h,o,g,e"), is("h\\,o\\,g\\,e"));

        // 組み合わせ
        assertThat(target.escape(",,\\,"), is("\\,\\,\\\\\\,"));
        assertThat(target.escape("\t,a,\\,"), is("\t\\,a\\,\\\\\\,"));

        // 空文字
        assertThat(target.escape(""), is(""));
    }

    @Test
    public void testCloseQuietly() {
        target.closeQuietly(null);  // NullPointerException must be ignored.
        target.closeQuietly(new Closeable() {
            public void close() throws IOException {
                throw new IOException("this exception must be ignored.");
            }
        });

        target.closeQuietly(new Closeable() {
            public void close() throws IOException {
            }
        });
    }

    private HSSFWorkbook open(File xls) throws IOException {
        InputStream in = new FileInputStream(xls);
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook book = new HSSFWorkbook(fs);
        in.close();
        return book;
    }

    private File createTempFile() throws IOException {
        File outFile = File.createTempFile(getClass().getName(), ".xls");
        outFile.deleteOnExit();
        return outFile;
    }
}
