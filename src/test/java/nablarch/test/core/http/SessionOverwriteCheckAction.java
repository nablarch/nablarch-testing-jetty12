package nablarch.test.core.http;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

public class SessionOverwriteCheckAction implements Handler<HttpRequest, HttpResponse> {
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        String val = ctx.getSessionScopedVar("otherSessionParam");
        if (val == null || !val.equals("hoge")) {
            throw new RuntimeException(val);   
        }
        
        val = ctx.getSessionScopedVar("commonHeaderLoginUserName");
        if (val == null || !val.equals("リクエスト単体テストユーザ2")) {
            throw new RuntimeException(val);   
        }
        
        val = ctx.getSessionScopedVar("commonHeaderLoginDate");
        if (val == null || !val.equals("20120914")) {
            throw new RuntimeException(val);   
        }

        // セッションを無効化
        ctx.invalidateSession();

        ctx.setSessionScopedVar("addKey", "これは追加される。");
        return new HttpResponse();
    }
}
