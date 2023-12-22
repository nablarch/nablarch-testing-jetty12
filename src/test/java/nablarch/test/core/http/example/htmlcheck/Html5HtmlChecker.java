package nablarch.test.core.http.example.htmlcheck;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.Charset;

import nablarch.core.util.Builder;
import nablarch.test.core.http.example.htmlcheck.html5parse.ParseException;
import nablarch.test.core.http.example.htmlcheck.html5parse.Parser;
import nablarch.test.core.http.example.htmlcheck.html5parse.SimpleNode;
import nablarch.test.core.http.example.htmlcheck.html5parse.TokenMgrError;
import nablarch.test.tool.htmlcheck.HtmlChecker;
import nablarch.test.tool.htmlcheck.HtmlForbiddenNodeConf;
import nablarch.test.tool.htmlcheck.InvalidHtmlException;
import nablarch.test.tool.htmlcheck.util.FileUtil;

/**
 * HTML5相当のチェックを行う HTMLチェックツール
 * @author Koichi Asano
 */
public class Html5HtmlChecker implements HtmlChecker {

    /**
     * デフォルトエンコーディング
     */
    public static final Charset DEFALUT_ENCODING = Charset.forName("UTF-8");
    
    /**
     * 使用を許可しないタグ・属性情報
     */
    private HtmlForbiddenNodeConf forbidden;

    /**
     * 規約上許可されていないタグ/属性が、HTML内で使われていないかをチェックするオブジェクト
     */
    private Html5ForbiddenChecker forbiddenChecker;

    /**
     * HTML ファイルのエンコーディング。<br/>
     * 
     * デフォルトは UTF-8
     */
    private Charset fileEncoding = DEFALUT_ENCODING;

    /**
     * コンストラクタ
     * 
     * @param confFilePath 設定ファイルパス
     */
    public Html5HtmlChecker() {
    }

    /**
     * Htmlファイルのチェックを行う。<br>
     * チェック内容は下記の通りである。<br>
     * <ul>
     * <li>正しい構文で記述されていること。</li>
     * <li>許可されていないタグが使用されていないこと。</li>
     * </ul>
     * @param html チェック対象HTMLファイル
     * @param encoding 指定文字コード
     * @throws InvalidHtmlException  チェック結果がNGの場合
     */
    protected void checkHtml(File html, Charset encoding) throws InvalidHtmlException {

        // 構文チェック
        SimpleNode node = parseHtml(html, encoding);
        
        try {
            // 使用禁止タグ・属性を使用していないことのチェック
            forbiddenChecker.check(node);
        } catch (InvalidHtmlException e) {
            throw new InvalidHtmlException(Builder.concat(
                    "forbidden tag or attribute detected. file = [", html, "] : " + e.getMessage()),
                    e);
        }
    }

    /**
     * {@inheritDoc}
     * @see nablarch.test.tool.htmlcheck.HtmlChecker#checkHtml(File)
     */
    public void checkHtml(File html) throws InvalidHtmlException {

        checkHtml(html, fileEncoding);
    }

    /**
     * HTMLの構文チェックを行う。
     * 
     * @param html チェック対象HTMLファイル
     * @param encoding 指定文字コード
     * @return HTMLファイルをパースした結果である構文木
     * @throws InvalidHtmlException  チェック結果がNGの場合
     */
    private SimpleNode parseHtml(File html, Charset encoding) throws InvalidHtmlException {
        
        try {

            Reader reader = null;
            try {

                reader = FileUtil.open(html, encoding);
                return new Parser(reader).document();

            } catch (TokenMgrError e) {
                throw new InvalidHtmlException(e);
            } catch (ParseException e) {
                throw new InvalidHtmlException(e);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            } finally {
                FileUtil.closeQuietly(reader);
            }
        } catch (InvalidHtmlException e) {
            throw new InvalidHtmlException(
                    Builder.concat("syntax check failed. file = [", html, "]"),
                    e);
        }
    }

    /**
     * HTMLファイルの文字コードを設定する。<br/>
     * 
     * 設定しなかった場合、 UTF-8 が使用される。
     * 
     * @param fileEncoding HTMLファイルの文字コード
     */
    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = Charset.forName(fileEncoding);
    } 

    /**
     * 設定ファイルのパスを設定する。
     * 
     * @param confFilePath 設定ファイルのパス
     */
    public void setConfFilePath(String confFilePath) {
        forbidden = new HtmlForbiddenNodeConf(confFilePath);
        forbiddenChecker = new Html5ForbiddenChecker(forbidden);

    }
}
