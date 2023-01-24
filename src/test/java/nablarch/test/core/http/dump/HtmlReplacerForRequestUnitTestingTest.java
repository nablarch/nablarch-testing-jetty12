package nablarch.test.core.http.dump;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;

/** {@link HtmlReplacerForRequestUnitTest}のテスト */
public class HtmlReplacerForRequestUnitTestingTest {

    /**
     * {@link HtmlReplacerForRequestUnitTest#main(String...)}のテスト<br/>
     * 入力ファイルの内容が書き換えられることを確認する。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testMainSuccess() throws IOException {
        // 入力ファイルの作成
        String in = "top: { \"action\": \"/action/MenuAction/MENUS00101;jsessionid=t39z5yw4gzjy1pgeousw0hie0\", \"allowDoubleSubmission\": true, \"params\": {} },";
        File infile = File.createTempFile(getClass().getName(), ".html");
        infile.deleteOnExit();
        FileWriter writer = new FileWriter(infile);
        writer.write(in);
        writer.close();

        // 出力ファイルの作成
        File outFile = File.createTempFile(getClass().getName(), ".html");
        // ターゲット実行
        HtmlReplacerForRequestUnitTesting.main(infile.getPath(), outFile.getPath());
        // 結果確認
        BufferedReader result = new BufferedReader(new FileReader(outFile));
        assertEquals(
                "top: { \"action\": \"http://localhost:57777//action/MenuAction/MENUS00101;jsessionid=t39z5yw4gzjy1pgeousw0hie0\", \"allowDoubleSubmission\": true, \"params\": {} },",
                result.readLine());
        result.close();
    }


    /**
     * {@link HtmlReplacerForRequestUnitTest#main(String...)}のテスト<br/>
     * プログラム引数が不足している場合に例外が発生すること
     * @throws IOException 予期しない例外
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMainTooFewArguments() throws IOException {
        HtmlReplacerForRequestUnitTesting.main("one");
    }

    /**
     * {@link HtmlReplacerForRequestUnitTest#main(String...)}のテスト<br/>
     * プログラム引数が超過している場合に例外が発生すること
     * @throws IOException 予期しない例外
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMainTooManyArguments() throws IOException {
        HtmlReplacerForRequestUnitTesting.main("one", "two", "three");
    }

    /**
     * コンストラクタのテスト
     * @throws Exception 予期しない例外
     */
    @Test
    public void testConstructor() throws Exception {
        Class<?> target = HtmlReplacerForRequestUnitTesting.class;
        Constructor constructor = target.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}

