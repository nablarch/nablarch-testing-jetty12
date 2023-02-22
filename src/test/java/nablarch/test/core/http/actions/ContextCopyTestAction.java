package nablarch.test.core.http.actions;

import nablarch.common.web.session.SessionUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * テストクラスで生成したExecutionContextが、
 * リクエスト単体テストサーバのコンテナで生成されたExecutionContextへとコピーされたことを確認するためのクラス。
 *
 * @author akihiko ookubo
 */
public class ContextCopyTestAction {

    /**
     * 往路でcontextに格納した値の検証と、値の書き換えを行うテスト用の業務Action。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doCopyAndChangeValue(HttpRequest request,
                                                                  ExecutionContext context) {

        //テスト側→コンテナ側に値がコピーされていることを確認する
        String val = context.getSessionScopedVar("sessionScope");
        if (val == null || !val.equals("sessionScope_value")) {
            throw new RuntimeException(val);
        }

        val = context.getRequestScopedVar("requestScope");
        if (val == null || !val.equals("requestScope_value")) {
            throw new RuntimeException(val);
        }

        val = SessionUtil.get(context,"sessionStore");
        if (val == null || !val.equals("sessionStore_value")) {
            throw new RuntimeException(val);
        }

        //往路でコンテナ側→テスト側に値がコピーされたことを確認ために、格納値を書き換える
        context.setRequestScopedVar("requestScope", "requestScope_value_change");
        context.setSessionScopedVar("sessionScope", "sessionScope_value_change");
        SessionUtil.put(context,"sessionStore", "sessionStore_value_change");

        //復路でテスト側のExecutionContextがクリアされたことを確認するために値をクリアする
        context.getRequestScopeMap().remove("requestScope_removeTarget");
        context.getSessionScopeMap().remove("sessionScope_removeTarget");
        SessionUtil.delete(context,"sessionStore_removeTarget");

        return new HttpResponse("/simple.html");
    }

    /**
     * 往路でcontextに格納した値の検証と、値の書き換えを行うテスト用の業務Action。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doSessionDelete(HttpRequest request,
                                             ExecutionContext context) {

        //テスト側→コンテナ側に値がコピーされていることを確認する
        String val = context.getSessionScopedVar("sessionScope_removeTarget");
        if (val == null || !val.equals("sessionScope_value_remove")) {
            throw new RuntimeException(val);
        }

        val = context.getRequestScopedVar("requestScope_removeTarget");
        if (val == null || !val.equals("requestScope_value_remove")) {
            throw new RuntimeException(val);
        }

        val = SessionUtil.get(context,"sessionStore_removeTarget");
        if (val == null || !val.equals("sessionStore_value_remove")) {
            throw new RuntimeException(val);
        }

        //復路でテスト側のExecutionContextがクリアされたことを確認するために値をクリアする
        context.getRequestScopeMap().remove("requestScope_removeTarget");
        context.getSessionScopeMap().remove("sessionScope_removeTarget");
        SessionUtil.delete(context,"sessionStore_removeTarget");

        return new HttpResponse("/simple.html");
    }

    /**
     * 往路でcontextに格納した値の検証と、Sessionの破棄を行うテスト用の業務Action。
     *
     * @param request リクエストコンテキスト
     * @param context HTTPリクエストの処理に関連するサーバ側の情報
     * @return HTTPレスポンス
     */
    public HttpResponse doInvalidateSession(HttpRequest request,
                                                                  ExecutionContext context) {

        // テスト側から引き渡されたcontextに値が格納されていることを確認する
        // この時点で値格納に失敗している場合は実行時エラーを送出
        String val = context.getSessionScopedVar("sessionScope");
        if (val == null || !val.equals("sessionScope_value")) {
            throw new RuntimeException(val);
        }

        val = SessionUtil.get(context,"sessionStore");
        if (val == null || !val.equals("sessionStore_value")) {
            throw new RuntimeException(val);
        }

        // SessionStoreを破棄する
        SessionUtil.invalidate(context);

        // Sessionを破棄する
        context.invalidateSession();

        return new HttpResponse("/simple.html");
    }
}
