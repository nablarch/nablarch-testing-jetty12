package nablarch.fw.web.httpserver;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * {@link HttpSession#invalidate()}の呼び出しを遅延させる{@link Filter}実装クラス。
 *
 * {@link nablarch.test.core.http.HttpRequestTestSupportHandler}では、
 * テストクラスとJetty上で実行されるテスト対象間での{@link nablarch.fw.ExecutionContext}のコピーを行っている。
 * テスト実行中にセッションがinvalidateされた場合、Jetty 9では{@link nablarch.fw.ExecutionContext}の
 * 書き戻し時に{@link IllegalStateException}がスローされてしまう。
 *
 * これを回避するためには、{@link HttpSession#invalidate()}が実行されるタイミングを遅らせる必要がある。
 * サーブレットフィルタ（本クラス）を差し込んで、ここで{@link HttpServletRequest}をラップする。
 * ラップした{@link HttpServletRequest}は、セッションを要求されると、やはりラップした{@link HttpSession}を返却する。
 * このラップした{@link HttpSession}では{@link HttpSession#invalidate()}が呼び出されても、実際にはinvalidateをせず、
 * invalidateが要求されたことを記録しておく。
 * 後続のすべての処理が終わった後、invalidateが要求された場合、実際にinvalidateを実行する。
 *
 * @author Taichi Uragami
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
        HttpServletRequest wrappedRequest = RequestWrapper.wrap((HttpServletRequest) request);
        chain.doFilter(wrappedRequest, response);

        RequestWrapper wrapper = (RequestWrapper) Proxy.getInvocationHandler(wrappedRequest);
        if (wrapper.isInvalidated()) {
            wrapper.invalidateSessionActually();
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * {@link HttpServletRequest}をラップする{@link InvocationHandler}実装クラス。
     */
    private static class RequestWrapper implements InvocationHandler, Runnable {

        /** {@link HttpServletRequest}の実体 */
        private final HttpServletRequest request;

        /** invalidateが要求されたかどうか */
        private boolean invalidated;

        /**
         * コンストラクタ。
         * @param request ラップ対象の{@link HttpServletRequest}
         */
        RequestWrapper(HttpServletRequest request) {
            this.request = request;
        }

        /**
         * 実際に{@link HttpSession#invalidate()}を実行する。
         */
        void invalidateSessionActually() {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        /**
         * {@inheritDoc}
         *
         * {@link HttpServletRequest#getSession()}または{@link HttpServletRequest#getSession(boolean)} が起動された場合、
         * {@link HttpSession}のラップを行う。
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.equals(HttpServletRequest.class.getDeclaredMethod("getSession"))
                    || method.equals(HttpServletRequest.class.getDeclaredMethod("getSession",
                            boolean.class))) {
                Object session = method.invoke(request, args);
                if (session == null) {
                    return null;
                }
                return SessionWrapper.wrap((HttpSession) session, this);
            }

            return method.invoke(request, args);
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

        /**
         * {@link HttpServletRequest}のラップを行う。
         * @param request ラップ対象の{@link HttpServletRequest}
         * @return ラップした{@link HttpServletRequest}
         */
        static HttpServletRequest wrap(HttpServletRequest request) {
            ClassLoader loader = request.getClass().getClassLoader();
            Class<?>[] interfaces = { HttpServletRequest.class };
            InvocationHandler h = new RequestWrapper(request);
            return (HttpServletRequest) Proxy.newProxyInstance(loader, interfaces, h);
        }
    }

    /**
     * {@link HttpSession}をラップする{@link InvocationHandler}実装クラス。
     */
    private static class SessionWrapper implements InvocationHandler {

        /** {@link HttpSession}の実体 */
        private final HttpSession session;

        /** invalidate起動時のコールバック */
        private final Runnable invalidationCallback;

        /**
         * コンストラクタ。
         * @param session ラップ対象の{@link HttpSession}
         * @param invalidationCallback invalidate起動時のコールバック
         */
        SessionWrapper(HttpSession session, Runnable invalidationCallback) {
            this.session = session;
            this.invalidationCallback = invalidationCallback;
        }

        /**
         * {@inheritDoc}
         *
         * {@link HttpSession#invalidate()}が起動された場合、予め登録されたコールバックを起動し、
         * Sessionの要素を全削除する。
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.equals(HttpSession.class.getDeclaredMethod("invalidate"))) {
                invalidationCallback.run();

                Enumeration<String> names = session.getAttributeNames();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    session.removeAttribute(name);
                }

                return null;
            }
            return method.invoke(session, args);
        }

        /**
         * {@link HttpSession}のラップを行う。
         * @param session ラップ対象の{@link HttpSession}
         * @param invalidationCallback invalidate起動時のコールバック
         * @return ラップした{@link HttpSession}
         */
        static HttpSession wrap(HttpSession session, Runnable invalidationCallback) {
            ClassLoader loader = session.getClass().getClassLoader();
            Class<?>[] interfaces = { HttpSession.class };
            InvocationHandler h = new SessionWrapper(session, invalidationCallback);
            return (HttpSession) Proxy.newProxyInstance(loader, interfaces, h);
        }
    }
}
