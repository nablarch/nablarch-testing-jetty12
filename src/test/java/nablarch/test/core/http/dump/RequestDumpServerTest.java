package nablarch.test.core.http.dump;

import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: TIS301686
 * Date: 11/02/01
 * Time: 19:54
 * To change this template use File | Settings | File Templates.
 */
public class RequestDumpServerTest {

    @Test(timeout = 10000)
    public void test() throws Exception {
        RequestDumpServer server = new RequestDumpServer();
        try {
            startServer(server);
            Thread.sleep(3000); // サーバ起動するまで待つ。
            RequestDumpServer.main(new String[0]);  // ２重起動しないこと
        } finally {
            server.shutdownServer();
            Thread.sleep(1000); // 終了するまで待つ。
        }
    }

    @Test(timeout = 10000)
    public void testStopServerFail() {
        RequestDumpServer server = new RequestDumpServer();
        try {
            server.stopServer(null);
            fail();
        } catch (RuntimeException e) {
            assertEquals(NullPointerException.class, e.getCause().getClass());
        }
    }

    private void startServer(final RequestDumpServer server) {
        new Thread() {
            @Override
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    @Test
    public void testCloseQuietly() throws IOException {
        RequestDumpServer server = new RequestDumpServer();
        server.closeQuietly(new ServerSocket() {
            @Override
            public void close() throws IOException {
                throw new IOException("this exception must be ignored");
            }
        });
    }

}
