package nablarch.fw.web.httpserver;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import jakarta.servlet.SessionTrackingMode;
import nablarch.test.core.http.HttpRequestTestSupportHandler;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee10.webapp.JspConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppConfiguration;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.ResourceLocator;
import org.eclipse.jetty.ee10.webapp.WebInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * Jetty12対応の{@link HttpServer}サブクラス。
 *
 * @author Taichi Uragami
 * @author Yutaka Kanayama
 * @author Tomoyuki Tanaka
 */
@Published(tag = "architect")
public class HttpServerJetty12 extends HttpServer {


    /** アプリケーションサーバの実体 */
    private Server jetty;

    /** 自動テスト実行用コネクター */
    private LocalConnector localConnector;

    /**
     * サーバを起動する。
     * <pre>
     * サーバスレッドを生成し、port()メソッドで指定されたポート番号上の
     * HTTPリクエストに対して処理を行う。
     * </pre>
     *
     * @return このオブジェクト自体
     */
    public HttpServerJetty12 start() {
        jetty = new Server(getPort());

        // 12.0.5 でembedded modeだけrelativeRedirectAllowed のデフォルト値が変更されている。
        // 以前の挙動を前提にテストコードを実装していると失敗してしまうため、設定を戻す。
        // https://github.com/jetty/jetty.project/issues/11947
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRelativeRedirectAllowed(false);
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);

        Connector conn = new ServerConnector(jetty, httpConnectionFactory);
        initialize(conn);
        try {
            jetty.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    /**
     * このサーバをテストモードで起動する。
     * @return このオブジェクト自体
     */
    public HttpServerJetty12 startLocal() {
        jetty = new Server();

        // 12.0.5 でembedded modeだけrelativeRedirectAllowed のデフォルト値が変更されている。
        // 以前の挙動を前提にテストコードを実装していると失敗してしまうため、設定を戻す。
        // https://github.com/jetty/jetty.project/issues/11947
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRelativeRedirectAllowed(false);
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);

        localConnector = new LocalConnector(jetty, httpConnectionFactory);
        initialize(localConnector);
        try {
            jetty.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * サーバスレッドが終了するまでカレントスレッドをwaitさせる。
     *
     * @return このオブジェクト自体
     */
    public HttpServer join() {
        try {
            jetty.join();

        } catch (InterruptedException e) {
            // カレントスレッドに割り込み要求を行ってから抜ける。
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <pre>
     * このクラスの実装では、
     * 引数のHTTPリクエストオブジェクトをHTTPメッセージにシリアライズし、
     * ローカルコネクションに送信する。
     * 内蔵アプリケーションサーバでの処理後、返信されたHTTPレスポンスメッセージを
     * HTTPレスポンスオブジェクトにパースし、この関数の戻り値として返す。
     * また、HTTPダンプ出力が有効である場合、
     * そのレスポンスボディの内容を所定のディレクトリに出力する。
     * </pre>
     */
    public HttpResponse handle(HttpRequest req, ExecutionContext sourceContext) {
        if (localConnector == null) {
            throw new RuntimeException(
                    "this server is not running on a local connector. "
                            + "you must call startLocal() method beforehand."
            );
        }

        String host = req.getHost();
        if (host == null || host.isEmpty()) {
            ((MockHttpRequest) req).setHost("127.0.0.1");
        }

        final CountDownLatch latch = new CountDownLatch(1);
        sourceContext.setRequestScopedVar(HttpRequestTestSupportHandler.NABLARCH_JETTY_CONNECTOR_LATCH, latch);
        try {
            byte[] rawReq = req.toString().getBytes();
            ByteBuffer response = localConnector.getResponse(ByteBuffer.wrap(rawReq));
            latch.await(10L, TimeUnit.SECONDS);
            byte[] rawRes = new byte[response.remaining()];
            response.get(rawRes);
            HttpResponse res = HttpResponse.parse(rawRes);
            if (isHttpDumpEnabled()) {
                dumpHttpMessage(req, res);
            }
            return res;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Jettyサーバインスタンスの初期化を行う。
     *
     * @param conn このサーバがacceptするコネクタ
     */
    private void initialize(Connector conn) {
        jetty.addConnector(conn);
        deploy();
    }

    /**
     * 内部サーバにWARをデプロイする。
     * <pre>
     * エントリポイントサーブレットと、
     * {@link #setWarBasePath(String)}で指定されたパス上に存在するWARをデプロイする。
     * </pre>
     */
    private void deploy() {
        WebAppContext webApp = new WebAppContext();
        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setSessionIdPathParameterName("none");
        // デフォルトは COOKIE と URL が設定されるが、 URL が設定されている場合 SessionIdPathParameterName が
        // none または未設定だと NullPointerException が発生してしまうので、 COOKIE だけに固定している
        sessionHandler.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
        webApp.setSessionHandler(sessionHandler);
        webApp.setContextPath(getServletContextPath());
        webApp.setBaseResource(toResourceCollection(getWarBasePaths()));
        webApp.setClassLoader(Thread.currentThread().getContextClassLoader());

        StandardJarScanner scanner = new StandardJarScanner();
        scanner.setScanManifest(false);
        webApp.setAttribute(JarScanner.class.getName(), scanner);

        webApp.setTempDirectoryPersistent(true);

        webApp.addFilter(LazySessionInvalidationFilter.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        Filter webFrontController = getWebFrontController();
        webApp.addFilter(
                new FilterHolder(webFrontController)
                , "/*"
                , EnumSet.of(DispatcherType.REQUEST)
        );

        Configuration[] configurations = {
                new WebAppConfiguration(),
                new JspConfiguration(),
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new AnnotationConfiguration()
        };
        webApp.setConfigurations(configurations);

        File tmpDir = getTempDirectory();
        if (tmpDir != null) {
            webApp.setTempDirectory(tmpDir);
        }

        jetty.setHandler(webApp);
    }

    /**
     * {@link ResourceLocator}のリストを{@link Resource}に変換する。
     * @param warBasePaths 変換元のリスト
     * @return 変換後の {@link Resource}
     */
    private Resource toResourceCollection(List<ResourceLocator> warBasePaths) {
        List<Resource> resources = new ArrayList<>(warBasePaths.size());
        try {
            for (ResourceLocator warBasePath : warBasePaths) {
                String resourcePath = warBasePath.getRealPath();
                Path normalizedResourcePath = new File(resourcePath).getCanonicalFile().toPath();
                resources.add(ResourceFactory.root().newResource(normalizedResourcePath));
            }
            return ResourceFactory.combine(resources);
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException(
                    "invalid warBasePath. " + warBasePaths, e);
        }
    }


}
