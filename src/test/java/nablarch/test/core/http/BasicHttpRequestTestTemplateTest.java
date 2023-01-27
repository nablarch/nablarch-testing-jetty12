package nablarch.test.core.http;

import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.httpserver.HttpServerJetty12;
import nablarch.test.RepositoryInitializer;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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
    }


    /**
     * テスト用の{@link BasicHttpRequestTestTemplate}
     * <p>
     * このクラスは、HTTPメソッドをテストデータで指定できるように拡張している。<br>
     * これは、Jetty12が静的リソースへのアクセスをPOSTでできなくなったことに起因する。
     * </p>
     * <p>
     * Jetty9までは、POSTメソッドでも静的リソースへアクセスできた。これは、DefaultServletのdoPostメソッドの
     * 実装がdoGetメソッドに処理を以上する形になっていたためできていた。<br>
     * しかし、Jetty12のDefaultServletはdoPostを実装しなくなったため、静的リソースにPOSTメソッドで
     * アクセスしようとすると405 Method Not Allowedのエラーが発生するようになった。
     * </p>
     * <p>
     * 本来はNTFを改良して任意のHTTPメソッドを指定できるようにすべきだが、
     * クエリパラメータの扱いをどうすべきかなど考慮しなければならないことが多いため、
     * 2023年の対応では一旦アドホックな対応で済ませることにした。<br>
     * NTFが改良されて標準機能として任意のHTTPメソッドを指定できるようになった場合は、
     * そちらを利用する方法に修正すること。
     * </p>
     */
    private static class BasicHttpRequestTestTemplateForTesting extends AbstractHttpRequestTestTemplate<BasicHttpRequestTestCaseInfo> {

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

        @Override
        protected HttpRequest createHttpRequest(BasicHttpRequestTestCaseInfo testCaseInfo) {
            final MockHttpRequest httpRequest = (MockHttpRequest)super.createHttpRequest(testCaseInfo);
            httpRequest.setMethod(testCaseInfo.getHttpMethod());
            return httpRequest;
        }

        @Override
        protected BasicHttpRequestTestCaseInfo createTestCaseInfo(
                String sheetName,
                Map<String, String> testCaseParams,
                List<Map<String, String>> contexts,
                List<Map<String, String>> requests,
                List<Map<String, String>> expectedResponses,
                List<Map<String, String>> cookie) {
            return new BasicHttpRequestTestCaseInfo(sheetName,
                    testCaseParams,
                    contexts,
                    requests,
                    expectedResponses,
                    cookie);
        }
    }

    /** {@link TestCaseInfo}を、HTTPメソッドを指定できるように拡張したクラス。 */
    private static class BasicHttpRequestTestCaseInfo extends TestCaseInfo {
        /** コンテキスト */
        private final List<Map<String, String>> context;

        public BasicHttpRequestTestCaseInfo(
                String sheetName,
                Map<String, String> testCaseParams,
                List<Map<String, String>> context,
                List<Map<String, String>> request,
                List<Map<String, String>> expectedResponseListMap,
                List<Map<String, String>> cookie) {
            super(sheetName, testCaseParams, context, request, expectedResponseListMap, cookie);
            this.context = context;
        }

        /**
         * テスト対象とするリクエストのHTTPメソッドを返却する。
         *
         * @return HTTPメソッド
         */
        public String getHttpMethod() {
            String httpMethod = context.get(0).get("HTTP_METHOD");
            return StringUtil.hasValue(httpMethod) ? httpMethod : "POST";
        }
    }

    /** テスト用HttpServer */
    private static class HttpServerForTesting extends HttpServerJetty12 {

        /** {@inheritDoc} */
        @Override
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            HttpResponse res = super.handle(req, ctx);
            // ステータスコードを設定する。
            HttpRequestTestSupport.getTestSupportHandler().setStatusCode(res.getStatusCode());
            return res;
        }
    }
}
