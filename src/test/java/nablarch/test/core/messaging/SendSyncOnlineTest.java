package nablarch.test.core.messaging;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.RepositoryInitializer;
import nablarch.test.core.http.BasicHttpRequestTestTemplate;
import nablarch.test.core.http.HttpRequestTestSupport;
import nablarch.test.core.http.HttpTestConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * 画面オンライン処理で取引単体のメッセージ同期送信を行うテスト。
 * @author Masato Inoue
 */
public class SendSyncOnlineTest extends BasicHttpRequestTestTemplate {

    /** {@inheritDoc} */
    @Override
    protected String getBaseUri() {
        return "/action/W11AD03Action/";
    }

    /** 初期化を行う */
    @BeforeClass
    public static void loadRepository() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/test/core/messaging/web/web-component-configuration.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    /** HttpTestConfigurationのリポジトリキー */
    private static final String HTTP_TEST_CONFIGURATION = "httpTestConfiguration";
    
    /**
     * 画面オンラインでメッセージ同期送信を行うテスト。
     */ 
    @Test
    public void testOnline() {

        String requestId = "RW11AD0102";
        // リクエストパラメータの形式に変換
        MockHttpRequest req = new MockHttpRequest();
        req.setParam("book.title", "test1");
        req.setParam("book.publisher", "test2");
        req.setParam("book.authors", "test3");
        
        ExecutionContext ctx = executeServer(requestId, req);
        
        // 1回目のメッセージ同期送信の応答メッセージのアサート
        SyncMessage responseMessage = ctx.getRequestScopedVar("responseRM11AD0101_1");
        // ヘッダ
        assertThat(responseMessage.getHeaderRecord().get("requestId").toString(), is("RM11AD0101"));
        assertThat(responseMessage.getHeaderRecord().get("resendFlag").toString(), is("9"));
        assertThat(responseMessage.getHeaderRecord().get("testCount").toString(), is("123"));
        assertNull(responseMessage.getHeaderRecord().get("reserved"));
        // 本文
        assertThat(responseMessage.getDataRecord().get("failureCode").toString(), is("test1"));
        assertThat(responseMessage.getDataRecord().get("userInfoId").toString(), is("user1"));
        
        // 2回目のメッセージ同期送信の応答メッセージのアサート
        SyncMessage responseMessage2 = ctx.getRequestScopedVar("responseRM11AD0101_2");
        // ヘッダ
        assertThat(responseMessage2.getHeaderRecord().get("requestId").toString(), is("RM11AD0101"));
        assertThat(responseMessage2.getHeaderRecord().get("resendFlag").toString(), is("9"));
        assertThat(responseMessage2.getHeaderRecord().get("testCount").toString(), is("124"));
        assertNull(responseMessage.getHeaderRecord().get("reserved"));
        // 本文
        assertThat(responseMessage2.getDataRecord().get("failureCode").toString(), is("test1_2"));
        assertThat(responseMessage2.getDataRecord().get("userInfoId").toString(), is("user1_1"));

        // 3回目のメッセージ同期送信の応答メッセージのアサート
        SyncMessage responseMessage3 = ctx.getRequestScopedVar("responseRM11AD0102");
        // ヘッダ
        assertThat(responseMessage3.getHeaderRecord().get("requestId").toString(), is("RM11AD0102"));
        assertThat(responseMessage3.getHeaderRecord().get("resendFlag").toString(), is(""));
        assertThat(responseMessage3.getHeaderRecord().get("testCount").toString(), is("123"));
        assertNull(responseMessage.getHeaderRecord().get("reserved"));
        // 本文
        assertThat(responseMessage3.getDataRecord().get("failureCode").toString(), is("test2"));
        assertThat(responseMessage3.getDataRecord().get("userInfoId").toString(), is("user2"));
        assertThat(responseMessage3.getDataRecord().get("test").toString(), is("hoge"));
        
    }
    
    /**
     * サーバを実行する。
     * @param requestId
     * @param req
     * @return
     */
    private ExecutionContext executeServer(String requestId, MockHttpRequest req) {

        // ExecutionContextの生成
        ExecutionContext ctx = new ExecutionContext();

        // HttpRequestの生成
        String uri = getBaseUri() + requestId;
        // リクエストパラメータの形式に変換
        req.setRequestUri(uri);    // URI設定
        req.setMethod("POST");
        
        // HTTPテスト実行用設定情報の取得
        HttpTestConfiguration config = (HttpTestConfiguration) SystemRepository.getObject(HTTP_TEST_CONFIGURATION);

        // 内蔵サーバ生成
        HttpServer server = createHttpServer(config);

        // HTTPヘッダーを設定する。
        setHttpHeader(req, config);
        
        // ExecutionContextの設定(ハンドラ実行中にExecutionContextの移送を行う為）
        HttpRequestTestSupport.getTestSupportHandler().setContext(ctx);
        // リポジトリの再初期化（指定された場合のみ）
        String xmlComponentFile = config.getXmlComponentFile();
        if (StringUtil.hasValue(xmlComponentFile)) {
            RepositoryInitializer.reInitializeRepository(xmlComponentFile);
        }
        
        // 実行
        server.handle(req, ctx);  // 第2引数は使用されない (テスト用に引渡し）
        
        return ctx;
    }
    
}
