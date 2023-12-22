package nablarch.fw.web.httpserver;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Enumeration;

/**
 * {@link HttpSession#invalidate()}の呼び出しを遅延させる{@link Filter}実装クラス。
 *
 * {@link nablarch.test.core.http.HttpRequestTestSupportHandler}では、
 * テストクラスとJetty上で実行されるテスト対象間での{@link nablarch.fw.ExecutionContext}のコピーを行っている。
 * テスト実行中にセッションがinvalidateされた場合、Jetty 12では{@link nablarch.fw.ExecutionContext}の
 * 書き戻し時に{@link IllegalStateException}がスローされてしまう。
 *
 * これを回避するためには、{@link HttpSession#invalidate()}が実行されるタイミングを遅らせる必要がある。
 * サーブレットフィルタ（本クラス）を差し込んで、ここで{@link HttpServletRequest}をラップする。
 * ラップした{@link HttpServletRequest}は、セッションを要求されると、やはりラップした{@link HttpSession}を返却する。
 * このラップした{@link HttpSession}では{@link HttpSession#invalidate()}が呼び出されても、実際にはinvalidateをせず、
 * invalidateが要求されたことを記録しておく。
 * 後続のすべての処理が終わった後、invalidateが要求された場合、実際にinvalidateを実行する。
 *
 * Jetty 9 ではラップに標準APIの Proxy を用いていたが、 Jetty 12 では単純なラップクラスを使用している。
 * これは、 Jetty 12 が内部で {@code instanceof} を使って{@link jakarta.servlet.ServletRequestWrapper} かどうか
 * 判定している部分があり、 Proxy を用いているとその判定条件に入れないという理由があるためである。
 *
 * @author Taichi Uragami
 * @author Tanaka Tomoyuki
 */
public class LazySessionInvalidationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * {@inheritDoc}
     *
     * {@link HttpSession}のラップを行う。
     * 後続処理終了後に遅延して{@link HttpSession#invalidate()}を行う。
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        RequestWrapper wrappedRequest = new RequestWrapper((HttpServletRequest)request);
        chain.doFilter(wrappedRequest, response);

        if (wrappedRequest.isInvalidated()) {
            wrappedRequest.invalidateSessionActually();
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * {@link HttpServletRequest}のラッパー。
     */
    private static class RequestWrapper extends HttpServletRequestWrapper implements Runnable {

        /** invalidateが要求されたかどうか */
        private boolean invalidated;
        public RequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession() {
            return this.getSession(true);
        }

        @Override
        public HttpSession getSession(boolean create) {
            HttpSession session = super.getSession(create);
            if (session == null) {
                return null;
            }
            return new SessionWrapper(session, this);
        }

        /**
         * 実際に{@link HttpSession#invalidate()}を実行する。
         */
        void invalidateSessionActually() {
            HttpSession session = super.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        @Override
        public void run() {
            invalidated = true;
        }

        /**
         * invalidateが要求されたか否かを判定する。
         * @return invalidateが要求された場合、真
         */
        boolean isInvalidated() {
            return invalidated;
        }
    }

    /**
     * {@link HttpSession} のラッパー。
     */
    private static class SessionWrapper implements HttpSession {
        /** {@link HttpSession}の実体 */
        private final HttpSession delegate;

        /** invalidate起動時のコールバック */
        private final Runnable invalidationCallback;

        private SessionWrapper(HttpSession delegate, Runnable invalidationCallback) {
            this.delegate = delegate;
            this.invalidationCallback = invalidationCallback;
        }

        @Override
        public void invalidate() {
            invalidationCallback.run();

            Enumeration<String> names = delegate.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                delegate.removeAttribute(name);
            }
        }

        @Override
        public long getCreationTime() {
            return delegate.getCreationTime();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public long getLastAccessedTime() {
            return delegate.getLastAccessedTime();
        }

        @Override
        public ServletContext getServletContext() {
            return delegate.getServletContext();
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            delegate.setMaxInactiveInterval(interval);
        }

        @Override
        public int getMaxInactiveInterval() {
            return delegate.getMaxInactiveInterval();
        }

        @Override
        public Object getAttribute(String name) {
            return delegate.getAttribute(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return delegate.getAttributeNames();
        }

        @Override
        public void setAttribute(String name, Object value) {
            delegate.setAttribute(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            delegate.removeAttribute(name);
        }

        @Override
        public boolean isNew() {
            return delegate.isNew();
        }
    }
}
