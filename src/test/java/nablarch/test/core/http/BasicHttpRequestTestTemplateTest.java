package nablarch.test.core.http;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.httpserver.HttpServerJetty12;
import nablarch.test.RepositoryInitializer;
import org.junit.After;
import org.junit.Test;


import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link BasicHttpRequestTestTemplate}のテストクラス。
 * 
 * @author Koichi Asano 
 *
 */
public class BasicHttpRequestTestTemplateTest {

    /** システムリポジトリとHttpRequestTestSupportをデフォルトに復元する。 */
    @After
    public void initializeSystemRepository() {
        RepositoryInitializer.revertDefaultRepository();
        HttpRequestTestSupport.resetHttpServer();
    }

    /** リダイレクトのステータスコードが同一視されることのテスト。予想結果302で実際302の場合 */
    @Test
    public void testRedirect() {
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/http-action-test-configuration.xml");
        // テスト用にサブクラス化
        BasicHttpRequestTestTemplate target = new BasicHttpRequestTestTemplate(getClass()) {

            @Override
            protected String getBaseUri() {
                return "/action/RedirectAction/";
            }
            
        };

        // 正しく終わるはず
        target.execute("redirect301");
        target.execute("redirect302");
        target.execute("redirect303");
        target.execute("redirect307");
        
        // 失敗するはず
        try {
            target.execute("redirect302303");
            fail("例外が発生するはず");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("expected:<30[3]> but was:<30[2]>"));
        }

        // 失敗するはず
        try {
            target.execute("redirect303200");
            fail("例外が発生するはず");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("expected:<[200]> but was:<[303]>"));
        }

        try {
            target.execute("redirect200303");
            fail("例外が発生するはず");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("expected:<[303]> but was:<[200]>"));
        }


        try {
            target.execute("redirect303400");
            fail("例外が発生するはず");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("expected:<[400]> but was:<[303]>"));
        }


        try {
            target.execute("redirect400303");
            fail("例外が発生するはず");
        } catch (AssertionError e) {
            // レスポンスコードが200系は
            assertThat(e.getMessage(), containsString("expected:<[303]> but was:<[400]>"));
        }
    }

    /**
     * オーバーレイのテスト。
     */
    @Test
    public void testOverlayFirst() {
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testOverlay.xml");
        BasicHttpRequestTestTemplateForTesting target = new BasicHttpRequestTestTemplateForTesting(getClass());
        target.execute("overlayFirst");   // 1番目のWebAppであるappのリソースを取得できること。
        target.execute("overlaySecond");  // 2番目のWebAppであるappのリソースを取得できること。
        target.execute("overlayThird");   // 3番目のWebAppであるappのリソースを取得できること。
        target.execute("overlayNotFound"); // 存在しないリソースは取得できないこと。
    }


    /** テスト用の{@link BasicHttpRequestTestTemplate} */
    private static class BasicHttpRequestTestTemplateForTesting extends BasicHttpRequestTestTemplate {

        private BasicHttpRequestTestTemplateForTesting(Class<?> testClass) {
            super(testClass);
        }

        @Override
        protected String getBaseUri() {
            return "/";
        }

        @Override
        protected HttpServer createHttpServer() {
            return new HttpServerForTesting();
        }
    }

    /** テスト用HttpServer */
    private static class HttpServerForTesting extends HttpServerJetty12 {

        /** {@inheritDoc}
         *  {@link HttpRequestTestSupportHandler#handle}内で、後続ハンドラから返却された
         *  {@link HttpResponse}のステータスコードを内部に保持している。
         *  しかし{@link HttpRequestTestSupportHandler#handle}で保持されるステータスコードは
         *  Nablarchが設定したステータスコードであり、その後コンテナが返却したレスポンスのステータスコードと一致しないことがある。
         *  ここではコンテナが返却したステータスコードを検証に使用したいため、{@link HttpServer}が返却したレスポンスを
         *  解析した後、ここで改めて{@link HttpRequestTestSupportHandler}のステータスコードにセットしている。
         */
        @Override
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            HttpResponse res = super.handle(req, ctx);
            // ステータスコードを設定する。
            HttpRequestTestSupport.getTestSupportHandler().setStatusCode(res.getStatusCode());
            return res;
        }
    }
}
