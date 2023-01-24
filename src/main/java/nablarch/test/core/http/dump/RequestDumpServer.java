package nablarch.test.core.http.dump;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletHandler;

/**
 * HTTPリクエストダンプ用サーバクラス。<br/>
 * <p>
 * 受信したHTTPリクエストパラメータをExcelファイルに記載し、
 * レスポンスとしてそのExcelファイルを返却する。
 * </p>
 * <p>
 * 本クラスはツールのmainメソッドを提供し、内蔵サーバ（サーブレットコンテナ）の起動を行う。
 * 実際の処理は、同パッケージの各クラスへと委譲される。
 * </p>
 *
 * @author T.Kawsaki
 */
public class RequestDumpServer {

    /** サーバに使用するポート番号 */
    static final int PORT_NUM = 57777;

    /** ポート番号 */
    private final int port;

    /** jettyサーバ */
    private Server jetty;

    /**
     * メインメソッド
     *
     * @param args 使用しない
     * @throws Exception 予期しない例外
     */
    public static void main(String[] args) throws Exception {
        new RequestDumpServer().start();
    }

    /** デフォルトコンストラクタ */
    public RequestDumpServer() {
        this(PORT_NUM);
    }

    /**
     * コンストラクタ
     *
     * @param port ポート番号
     */
    public RequestDumpServer(int port) {
        this.port = port;
        this.jetty = createServer();
    }

    /**
     * 開始する。
     *
     * @throws Exception 想定しない例外
     */
    public void start() throws Exception {
        if (isAlreadyInUse()) {
            return;
        }
        jetty.start();
        jetty.join();
    }


    /**
     * アプリケーションサーバインスタンスを生成する。
     *
     * @return サーバインスタンス
     */
    private Server createServer() {

        Server server = new Server(port);
        // コネクタの設定
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);
        // ハンドラの設定
        ServletHandler handler = new ServletHandler();
        // Filterを登録
        FilterHolder filterHolder = new FilterHolder(new RequestDumpServerShutdownFilter(this));
        handler.addFilterWithMapping(filterHolder, "/shutdown", EnumSet.allOf(DispatcherType.class));
        // Servletを登録
        handler.addServletWithMapping(RequestDumpServlet.class, "/");
        server.setHandler(handler);
        return server;
    }

    /** サーバをシャットダウンする */
    void shutdownServer() {
        Thread t = new Thread() {
            @Override
            public void run() {
                stopServer(jetty);
            }
        };
        t.start();
    }

    /**
     * サーバを停止する。
     * @param server jettyサーバ
     */
    void stopServer(Server server) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ポートが既に使用中かどうか判定する。
     *
     * @return 使用中の場合はtrue、そうでない場合はfalse
     * @throws IOException 予期しない例外
     */
    private boolean isAlreadyInUse() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (BindException alreadyBounded) {
            System.err.println("port " + port + " is already used.");
            return true;
        } finally {
            closeQuietly(serverSocket);
        }
        return false;
    }


    /**
     * 例外発生なしでソケットを閉じる
     *
     * @param socket クローズ対象のソケット
     */
    void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {  // SUPPRESS CHECKSTYLE
                // NOP
            }
        }
    }
}
