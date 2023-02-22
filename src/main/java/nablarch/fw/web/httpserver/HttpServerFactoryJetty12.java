package nablarch.fw.web.httpserver;

import nablarch.fw.web.HttpServer;
import nablarch.fw.web.HttpServerFactory;

/**
 * Jetty12対応の{@link HttpServer}を生成するファクトリ実装クラス。
 *
 * @author Taichi Uragami
 * @author Tomoyuki Tanaka
 */
public class HttpServerFactoryJetty12 implements HttpServerFactory {

    @Override
    public HttpServer create() {
        return new HttpServerJetty12();
    }
}
