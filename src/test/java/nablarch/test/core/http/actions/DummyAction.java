package nablarch.test.core.http.actions;

import nablarch.core.db.support.DbAccessSupport;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * HTML出力テスト用クラス。
 *
 * @author Koichi Asano 
 *
 */
public class DummyAction extends DbAccessSupport {


    /**
     * JSPにフォワードする。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doForwardJSP(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse("/index.jsp");
    }

    /**
     * html5形式のHTMLファイルを返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doForwardHtml5HTML(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse("/validHtml5.html");
    }


    /**
     * シンプルなHTMLファイルを返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doForwardSimpleHTML(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse("/simple.html");
    }

}
