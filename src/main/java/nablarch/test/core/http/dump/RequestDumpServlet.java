package nablarch.test.core.http.dump;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;

/**
 * HTTPダンプ用サーブレットクラス。<br/>
 * <p>
 * HTTPパラメータをExcelファイルに出力し、そのファイルをレスポンスとして返却する。
 * 本クラスを任意のURI（例えば、/）にマッピングすることで、URIに対するHTTPリクエストパラメータを
 * キャプチャーすることができる。
 * </p>
 * <p>
 * 本クラスはHTTPに関する処理のみを行い、実際のExcelファイル作成処理は{@link RequestDumpAgent}に委譲する。
 * </p>
 * @author T.Kawasaki
 */
public class RequestDumpServlet extends HttpServlet {

    /** ダンプファイル名 */
    private static final String FILE_NAME = "http_request_dump.xls";

    /** {@inheritDoc} */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String fileName = URLEncoder.encode(FILE_NAME, "UTF-8");
        res.setContentType("application/vnd.ms-excel");
        res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        @SuppressWarnings("unchecked")
        Map<String, String[]> params = req.getParameterMap();
        String uri = req.getRequestURI();
        RequestDumpAgent requestDumpAgent = new RequestDumpAgent();
        OutputStream out = res.getOutputStream();
        requestDumpAgent.print(uri, params, out);
    }
}
