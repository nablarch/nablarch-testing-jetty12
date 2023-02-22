package nablarch.test.core.http.dump;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@link RequestDumpServerShutdownFilter}のテスト
 *
 * @author T.Kawasaki
 */
public class RequestDumpServerShutdownFilterTest {


    /**
     * {@link RequestDumpServerShutdownFilter#doFilter(jakarta.servlet.ServletRequest,
     *     jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)} のテスト<br/>
     * 202レスポンスが返却されること
     *
     * @throws Exception 予期しない例外
     */
    @Test(timeout = 10000)
    public void testDoFilter() throws Exception {

        // サーバ起動
        new Thread() {
            public void run() {
                try {
                    RequestDumpServer.main(new String[0]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        Thread.sleep(3000);    // 待機
        // シャットダウン要求
        String req = "GET /shutdown HTTP/1.0\r\n"
                + "\r\n";
        List<String> res = submit(req);
        // レスポンス確認
        assertEquals("HTTP/1.1 202 Accepted", res.get(0));

        // シャットダウン要求再実行
        Thread.sleep(3000);
        try {
            submit(req);
        } catch (java.net.ConnectException e) {
            return;  // サーバが停止している。
        }
        fail("サーバが停止していません。");
    }

    /**
     * サブミットする。
     *
     * @param req HTTPリクエスト
     * @return レスポンス
     * @throws IOException 予期しない例外
     */
    private List<String> submit(String req) throws IOException {
        Socket socket = new Socket("localhost", RequestDumpServer.PORT_NUM);
        try {
            Reader reader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            Writer writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            writer.write(req);
            writer.flush();
            return readAll(reader);
        } finally {
            socket.close();
        }
    }

    /**
     * 全部読み取る。
     * @param reader 読み取り対象
     * @return 読み取り結果
     * @throws IOException 予期しない例外
     */
    private List<String> readAll(Reader reader) throws IOException {
        BufferedReader r = new BufferedReader(reader);
        List<String> result = new ArrayList<String>();
        String line;
        while ((line = r.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
