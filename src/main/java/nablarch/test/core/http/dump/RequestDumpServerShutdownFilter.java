package nablarch.test.core.http.dump;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * HTTPリクエストダンプ用サーバをシャットダウンする為のサーブレットフィルタクラス。<br/>
 * 特定のURL（例えば、/shutdown）に本フィルタをマッピングすることで、
 * ブラウザからサーバのシャットダウン指示が可能となる。
 *
 * @author T.Kawasaki
 */
public class RequestDumpServerShutdownFilter implements Filter {

    /** シャットダウン対象のインスタンス */
    private final RequestDumpServer requestDumpServer;

    /**
     * コンストラクタ。
     *
     * @param requestDumpServer シャットダウン対象のインスタンス
     */
    RequestDumpServerShutdownFilter(RequestDumpServer requestDumpServer) {
        this.requestDumpServer = requestDumpServer;
    }

    /**
     * {@inheritDoc}
     * 処理無し。
     */
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * {@inheritDoc}
     * サーバのシャットダウンを行う。レスポンス（ステータスコード202）を返却し、
     * 後続のフィルタへの処理委譲は行わ<b>ない</b>。
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) servletResponse;
        res.setStatus(202);
        res.setContentType("text/plain");
        PrintWriter w = res.getWriter();
        w.println("shutdown request is accepted.");
        w.close();
        requestDumpServer.shutdownServer();
    }

    /**
     * {@inheritDoc}
     * 処理無し。
     */
    public void destroy() {
    }
}
