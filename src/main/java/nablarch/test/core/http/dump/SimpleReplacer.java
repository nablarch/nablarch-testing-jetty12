package nablarch.test.core.http.dump;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * 単純な置換を実行するクラス。<br/>
 * 以下の3種類の入出力をサポートする。
 * <ul>
 * <li>File -> File</li>
 * <li>InputStream -> OutputStream</li>
 * <li>Reader -> Writer<li>
 * <ul>
 * 入力した各行について、指定された置換対象パターンに合致するかどうか判定し、
 * 合致した場合は、指定された置換文字列に置き換えて出力する。
 * 置換対象パターンに合致しなかった場合は、入力した行をそのまま出力する。
 *
 * @author T.Kawasaki
 */
public class SimpleReplacer {

    /** 置換対象パターン */
    private final Pattern pattern;

    /** 置換文字列 */
    private final String replacement;

    /** エンコーディング */
    private final Charset charset;

    /** 行セパレータ */
    private final String lineSeparator;


    /**
     * コンストラクタ。<br/>
     * 改行文字は、システムデフォルトのものが使用される。
     *
     * @param pattern     置換対象パターン
     * @param replacement 置換文字列
     * @param charsetName ファイルエンコーディング
     */
    public SimpleReplacer(Pattern pattern, String replacement, String charsetName) {
        this(pattern, replacement, charsetName, System.getProperty("line.separator"));
    }

    /**
     * フルコンストラクタ。<br/>
     *
     * @param pattern       置換対象パターン
     * @param replacement   置換文字列
     * @param charsetName   ファイルエンコーディング
     * @param lineSeparator 行セパレータ
     */
    public SimpleReplacer(Pattern pattern, String replacement, String charsetName, String lineSeparator) {
        this(pattern, replacement, Charset.forName(charsetName), lineSeparator);
    }

    /**
     * フルコンストラクタ。<br/>
     *
     * @param pattern       置換対象パターン
     * @param replacement   置換文字列
     * @param charset       ファイルエンコーディング
     * @param lineSeparator 行セパレータ
     */
    public SimpleReplacer(Pattern pattern, String replacement, Charset charset, String lineSeparator) {
        this.pattern = pattern;
        this.replacement = replacement;
        this.charset = charset;
        this.lineSeparator = lineSeparator;
    }


    /**
     * 書き換えを実行する。<br/>
     * ファイルを対象とした置換を行う場合は本メソッドを使用する。
     *
     * @param inFilePath  入力元ファイル
     * @param outFilePath 出力先ファイル
     * @throws IOException 入力ファイルが存在しない場合
     */
    public void replace(String inFilePath, String outFilePath) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(inFilePath);
            out = new FileOutputStream(outFilePath);
            replace(in, out);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /**
     * 書き換えを実行する。<br/>
     * ストリームを対象とした置換を行う場合は本メソッドを使用する。
     * ストリームのクローズは呼び出し元で行うこと。
     *
     * @param in  入力ストリーム
     * @param out 出力ストリーム
     * @throws IOException 入出力例外
     */
    public void replace(InputStream in, OutputStream out) throws IOException {
        Reader reader = new InputStreamReader(in, charset);
        Writer writer = new OutputStreamWriter(out, charset);
        replace(reader, writer);
    }


    /**
     * 書き換えを実行する。<br/>
     * リーダを対象とした置換を行う場合は本メソッドを使用する。
     * リーダ、ライタのクローズは呼び出し元で行うこと。
     *
     * @param reader 入力元リーダ
     * @param writer 出力先ライター
     * @throws IOException 入出力例外
     */
    public void replace(Reader reader, Writer writer) throws IOException {
        replace(new BufferedReader(reader), new BufferedWriter(writer));
    }

    /**
     * 書き換えを実行する。<br/>
     *
     * @param reader 入力元リーダ
     * @param writer 出力先ライター
     * @throws IOException 入出力例外
     */
    protected void replace(BufferedReader reader, BufferedWriter writer) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String replaced = replaceLine(line);
            writer.write(replaced);
            writer.write(lineSeparator);
        }
        writer.flush();
    }

    /**
     * 行の書き換えを実行する。<br/>
     * <p/>
     * 行毎の置き換えロジックを変更する場合は、このメソッドをオーバライドする。
     *
     * @param in 入力行
     * @return 出力行
     */
    protected String replaceLine(String in) {
        return pattern.matcher(in).replaceAll(replacement);
    }

    /**
     * 例外発生なしでクローズする。<br/>
     *
     * @param closeable クローズ対象
     */
    void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignored) {     // SUPPRESS CHECKSTYLE
            // NOP
        }
    }
}
