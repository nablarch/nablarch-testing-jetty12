package nablarch.test.core.http.actions;

import nablarch.core.db.support.DbAccessSupport;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * リダイレクトテスト用クラス。
 *
 * @author Koichi Asano 
 *
 */
public class RedirectAction extends DbAccessSupport {

    /**
     * 301を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect301(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(301);
    }
    
    /**
     * 303を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect303(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(303);
    }

    /**
     * 302を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect302(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(302);
    }
    
    /**
     * 307を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect307(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(307);
    }

    /**
     * 200を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect200(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(200);
    }

    /**
     * 400を返す。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doRedirect400(HttpRequest request,
            ExecutionContext context) {
        return new HttpResponse(400);
    }
}
