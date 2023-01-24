package nablarch.test.core.http.dump;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * リクエスト単体テスト用HTML置換クラス<br/>
 * <p>
 * リクエスト単体テストで生成されたHTMLファイルを入力し、
 * サブミット先をリクエストダンプサーバに書き換える。
 * JavaScript(JSON)上のサブミット先URIを置換することで、
 * 全てのサブミットをhttp://localhostへ送信させる。
 * </p>
 * 例えば、HTML中に以下のようなJSONが存在した場合、
 * <pre>
 *   nablarch_submission_info.nablarch_form1 = {
 *     top: { "action": "/action/MenuAction/MENUS00101", "allowDoubleSubmission": true, "params": {} },
 *     logout: { "action": "/action/LoginAction/LOGIN00101", "allowDoubleSubmission": true, "params": {} }
 *   };
 * </pre>
 * 次のように置き換えられる。
 * <pre>
 *   nablarch_submission_info.nablarch_form1 = {
 *     top: { "action": "http://localhost:57777/action/MenuAction/MENUS00101", "allowDoubleSubmission": true, "params": {} },
 *     logout: { "action": "http://localhost:57777/action/LoginAction/LOGIN00101", "allowDoubleSubmission": true, "params": {} }
 *   };
 * </pre>
 *
 * @author T.Kawasaki
 */
public final class HtmlReplacerForRequestUnitTesting {

    /** パターン */
    private static final Pattern PATTERN = Pattern.compile("(\"*action\"*:\\s*\")([^\"]*\")");

    /** 置換文字列 */
    private static final String REPLACEMENT = "$1http://localhost:" + RequestDumpServer.PORT_NUM + "/$2";

    /**
     * メインメソッド
     *
     * @param args 第１引数：入力元ファイルパス、第２引数：出力先ファイルパス
     * @throws IOException 入力元ファイルが存在しない場合
     */
    public static void main(String... args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("invalid program argument");
        }
        String inFilePath = args[0];
        String outFilePath = args[1];
        SimpleReplacer replacer = new SimpleReplacer(PATTERN, REPLACEMENT, "UTF-8");
        replacer.replace(inFilePath, outFilePath);
    }

    /** プライベートコンストラクタ */
    private HtmlReplacerForRequestUnitTesting() {
    }
}
