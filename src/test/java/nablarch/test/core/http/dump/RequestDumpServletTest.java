package nablarch.test.core.http.dump;

import org.junit.Test;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link RequestDumpServlet}のテスト
 *
 * @author T.Kawasaki
 */
public class RequestDumpServletTest {

    /**
     * {@link RequestDumpServlet#service(HttpServletRequest, HttpServletResponse)}のテスト
     * <p/>
     * Excelのダウンロードが行われること。
     * ファイルの中身は{@link RequestDumpAgentTest}で確認する。
     * 本メソッドではHttpResponseヘッダの確認を行う。
     *
     * @throws IOException      予期しない例外
     * @throws ServletException 予期しない例外
     */
    @Test
    public void testService() throws IOException, ServletException {
        // HttpServletRequest生成
        HttpServletRequest req = createMock(HttpServletRequest.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if (methodName.equals("getRequestURI")) {
                    return "/requestUri";
                } else if (methodName.equals("getParameterMap")) {
                    return new HashMap<String, String[]>();
                }
                return null;
            }
        });

        // HttpServletResponse生成
        final StringBuilder contentType = new StringBuilder();
        final Map<Object, Object> header = new HashMap<Object, Object>();
        HttpServletResponse res = createMock(HttpServletResponse.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if (methodName.equals("setContentType")) {
                    contentType.append(args[0]);
                } else if (methodName.equals("setHeader")) {
                    header.put(args[0], args[1]);
                } else if (methodName.equals("getOutputStream")) {
                    return new ServletOutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            // NOP
                        }
                        @Override
                        public boolean isReady() {
                            // NOP
                            return false;
                        }
                        @Override
                        public void setWriteListener(WriteListener writeListener) {
                            // NOP
                        }
                    };
                }
                return null;
            }
        });
        // ターゲット実行
        RequestDumpServlet target = new RequestDumpServlet();
        target.service(req, res);
        // レスポンス確認
        assertEquals("application/vnd.ms-excel", contentType.toString());
        assertEquals("attachment; filename=\"http_request_dump.xls\"", header.get("Content-Disposition"));
    }


    /**
     * モックを作成する。
     *
     * @param targetClass モック対象クラス
     * @param handler     InvokationHandler実装
     * @return モック
     */
    @SuppressWarnings("unchecked")
    public static <T> T createMock(Class targetClass, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                new Class[]{targetClass},
                handler);
    }


}
