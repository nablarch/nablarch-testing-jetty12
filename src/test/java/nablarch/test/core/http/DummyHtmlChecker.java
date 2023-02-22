package nablarch.test.core.http;

import nablarch.test.tool.htmlcheck.HtmlChecker;

import java.io.File;

public class DummyHtmlChecker implements HtmlChecker {

    private boolean called = false;
    public void checkHtml(File html) {
        called = true;
    }

    public boolean isCalled() {
        return called;
    }
}
