package nablarch.test.core.http;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.common.web.session.SessionStoreHandler;
import nablarch.common.web.session.SessionUtil;
import nablarch.common.web.session.store.DbStore;
import nablarch.core.date.SystemTimeProvider;
import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.BasicStringResource;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.util.FileUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.ResourceLocator;
import nablarch.fw.web.handler.SessionConcurrentAccessHandler;
import nablarch.fw.web.handler.SessionConcurrentAccessHandler.ConcurrentAccessPolicy;
import nablarch.fw.web.upload.PartInfo;
import nablarch.test.IgnoringLS;
import nablarch.test.NablarchTestUtils;
import nablarch.test.RepositoryInitializer;


import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.tool.Hereis;
import nablarch.test.tool.htmlcheck.HtmlChecker;
import nablarch.test.tool.htmlcheck.InvalidHtmlException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link HttpRequestTestSupport}のテストクラス
 *
 * @author hisaaki sioiri
 * @author Masato Inoue
 */
@RunWith(DatabaseTestRunner.class)
public class HttpRequestTestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    private final File destDir = new File("./tmp");

    @Before
    public void setUp() throws Exception {
        clearServer();
        VariousDbTestHelper.createTable(UserSession.class);
        delete(destDir);
        destDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        HttpRequestTestSupport.jsTestResourcePath = null;
        delete(destDir);
    }

    /**
     * 終了時にtmpディレクトリを削除する。
     *
     * @throws IOException
     */
    @AfterClass
    public static void classDown() throws Exception {
        clearServer();
    }

    private static void clearServer() throws NoSuchFieldException, IllegalAccessException {
        Field field = HttpRequestTestSupport.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(null, false);
    }


    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。
     *
     * @throws Exception
     */
    @Test
    public void testExecute() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute01.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        // バックアップディレクトリが存在する場合に、それらのディレクトリは一度削除されることを確認する。
        File bkDir = new File(destDir, "test_dump_bk/HttpRequestTestSupportExtends");
        bkDir.mkdirs();
        assertTrue(bkDir.exists());
        File toBeDel = new File(bkDir, "toBeDel");
        toBeDel.createNewFile();
        assertTrue(toBeDel.exists());

        System.setProperty("nablarch.test.skip-resource-copy", "true");
        try {
            target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());
        } finally {
            System.clearProperty("nablarch.test.skip-resource-copy");
        }

        String path = "test_dump/HttpRequestTestSupportExtends";
        File dumpDir = new File(destDir, path);

        // cssがコピーされることの確認
        assertTrue(new File(dumpDir, "css/sample01.css").exists());
        assertTrue(new File(dumpDir, "css/sample02.css").exists());
        assertTrue(new File(dumpDir, "action/sample.css").exists());
        assertTrue(new File(dumpDir, "action/sample/sample.css").exists());
        // jpgがコピーされることの確認
        assertTrue(new File(dumpDir, "img/sample01.jpg").exists());
        assertTrue(new File(dumpDir, "img/sample02.jpg").exists());

        // ignoreDirの配下はコピーされないこと。
        assertFalse(new File(dumpDir, "action/ignoreDir").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir/ignoreFile1.css").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir2").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir2/ignoreFile1.css").exists());
        assertFalse(new File(dumpDir, "ignoreDir").exists());
        assertFalse(new File(dumpDir, "ignoreDir/ignore.js").exists());

        // バックアップディレクトリ内のファイルは削除されていること
        assertFalse(toBeDel.exists());
    }

    /**
     * オーバレイのテスト。
     *
     * @throws IOException
     */
    @Test
    public void testOverlay() throws IOException {
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testOverlay.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends2();

        System.setProperty("nablarch.test.skip-resource-copy", "true");
        try {
            target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());
        } finally {
            System.clearProperty("nablarch.test.skip-resource-copy");
        }

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends2");


        // htmlがコピーされることの確認
        assertTrue(new File(dumpDir, "first.html").exists());
        assertTrue(new File(dumpDir, "second.html").exists());
        assertTrue(new File(dumpDir, "third.html").exists());

        // 重複したリソースの確認
        File duplicateHtml = new File(dumpDir, "duplicate.html");
        assertThat(duplicateHtml.exists(), is(true));
        // 先頭のwarディレクトリにあるファイルがコピーされていること
        assertThat(fileToString(duplicateHtml), is("this is resource of first module."));

        File duplicate2Html = new File(dumpDir, "duplicate2.html");
        assertThat(duplicate2Html.exists(), is(true));
        // 先頭のwarディレクトリにあるファイルがコピーされていること
        assertThat(fileToString(duplicate2Html), is("this is resource of second module."));
    }


    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。
     *
     * @throws Exception
     */
    @Test
    public void testExecute2() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute01-2.xml");

        File webDir = new File("src/test/resources/nablarch/test/core/http/web");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends");
        File destFile = new File(dumpDir, "css/sample02.css");
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        destFile.createNewFile();
        destFile.setLastModified(new File(webDir, "css/sample02.css").lastModified() - 1);

        destFile = new File(dumpDir, "img/sample01.jpg");
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        destFile.createNewFile();
        long lastModified = new File(webDir, "img/sample01.jpg").lastModified();
        destFile.setLastModified(lastModified);

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();
        target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());

        // cssがコピーされることの確認
        assertTrue(new File(dumpDir, "css/sample01.css").exists());
        assertTrue(new File(dumpDir, "action/sample.css").exists());
        assertTrue(new File(dumpDir, "action/sample/sample.css").exists());
        // jpgがコピーされることの確認
        assertTrue(new File(dumpDir, "img/sample01.jpg").exists());
        assertTrue(new File(dumpDir, "img/sample02.jpg").exists());

        //上書きコピーの確認
        assertThat("タイムスタンプが異なるので上書きコピーされること", new File(dumpDir, "css/sample02.css").lastModified(), is(new File(webDir,
                "css/sample02.css").lastModified()));
        assertThat("タイムスタンプが同一なのでコピーされないこと", new File(dumpDir, "img/sample01.jpg").length(), is(0L));

        // ignoreDirの配下はコピーされないこと。
        assertFalse(new File(dumpDir, "action/ignoreDir").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir/ignoreFile1.css").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir2").exists());
        assertFalse(new File(dumpDir, "action/ignoreDir2/ignoreFile1.css").exists());
        assertFalse(new File(dumpDir, "ignoreDir").exists());
        assertFalse(new File(dumpDir, "ignoreDir/ignore.js").exists());
    }


    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。ExtensionListが空のパターン。
     */
    @Test
    public void testExecuteSkipResourceCopy() throws IOException {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute03.xml");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends");
        dumpDir.mkdirs();
        assertTrue(dumpDir.exists());
        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        System.setProperty("nablarch.test.skip-resource-copy", "true");
        try {
            target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());
        } finally {
            System.clearProperty("nablarch.test.skip-resource-copy");
        }
        // cssがコピーされることの確認
        assertTrue(new File(dumpDir, "css/sample01.css").exists());
        assertTrue(new File(dumpDir, "css/sample02.css").exists());
        assertTrue(new File(dumpDir, "action/sample.css").exists());
        assertTrue(new File(dumpDir, "action/sample/sample.css").exists());
        // jpgがコピーされないことの確認
        assertFalse(new File(dumpDir, "img/sample01.jpg").exists());
        assertFalse(new File(dumpDir, "img/sample02.jpg").exists());
    }


    /**
     * /** {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。ExtensionListが空のパターン。
     *
     * @throws Exception
     */
    @Test
    public void testExecuteExtensionListEmpty() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute02.xml");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        target.execute(this.getClass(), "testExecute", new MockHttpRequest(), new ExecutionContext());

        // cssがコピーされないことの確認
        assertFalse(new File(dumpDir, "css/sample01.css").exists());
        assertFalse(new File(dumpDir, "css/sample02.css").exists());
        assertFalse(new File(dumpDir, "action/sample.css").exists());
        assertFalse(new File(dumpDir, "action/sample/sample.css").exists());
        // jpgがコピーされないことの確認
        assertFalse(new File(dumpDir, "img/sample01.jpg").exists());
        assertFalse(new File(dumpDir, "img/sample02.jpg").exists());
    }


    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 異常系。HtmlResourcesRootが空のパターン。
     *
     * @throws Exception
     */
    @Test
    public void testExecuteHtmlResourcesRootEmpty() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute04.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        try {
            target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 異常系。HtmlResourcesRootがnullのパターン。
     *
     * @throws Exception
     */
    @Test
    public void testExecuteHtmlResourcesRootNull() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute05.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        try {
            target.execute(this.getClass(), "testExecute", new MockHttpRequest(),
                    new ExecutionContext());
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }


    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。スタイルシートの中身が置換されることを確認する。
     *
     * @throws Exception
     */
    @Test
    public void testExecuteReplaceCss() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute01.xml");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportTest");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        target.execute(this.getClass(), "testExecute", new MockHttpRequest(), new ExecutionContext());

        // スタイルシートの中身が置換されていることの確認
        File cssFile01 = new File(dumpDir, "css/sample01.css");
        assertThat(Hereis.string()
                .trim(), IgnoringLS.equals(fileToString(cssFile01)
                .trim()));
        /*****************************************************
        #test01 {
            background-image: url("../img/sample01.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }

        #test02 {
            background-image: url("../img/sample02.jpg");
            background-repeat: no-repeat;
            background-color: #dcdcdc;
            border-top: solid 0.1em;
            position: relative;
            bottom: 0em;
            height: 3.8em;
            line-height: 3.8em;
            padding-left: 5em;
            overflow: hidden;
        }

        #test10 {
            background-image: url(  "../img/sample01.jpg"  );
            background-repeat: no-repeat;
            background-color: #dcdcdc;
            border-top: solid 0.1em;
            position: relative;
            bottom: 0em;
            height: 3.8em;
            line-height: 3.8em;
            padding-left: 5em;
            overflow: hidden;
        }

        #test11 {
            background-image: url( '../img/sample01.jpg' );
            background-repeat: no-repeat;
            background-color: #dcdcdc;
            border-top: solid 0.1em;
            position: relative;
            bottom: 0em;
            height: 3.8em;
            line-height: 3.8em;
            padding-left: 5em;
            overflow: hidden;
        }

        #test12 {
            background-image: url( ../img/sample01.jpg );
            background-repeat: no-repeat;
            background-color: #dcdcdc;
            border-top: solid 0.1em;
            position: relative;
            bottom: 0em;
            height: 3.8em;
            line-height: 3.8em;
            padding-left: 5em;
            overflow: hidden;
        }
        ****************************************************/

        File cssFile02 = new File(dumpDir, "css/sample02.css");
        assertThat(Hereis.string()
                .trim(), IgnoringLS.equals(fileToString(cssFile02)
                .trim()));
        /*****************************************************
        #test03 {
            background-image: url("./img/test.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }

        #test04 {
            background-image: url("../../img/test.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }

        #test05 {
            background-image: url("img/test.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }
        ****************************************************/


        File cssFile03 = new File(dumpDir, "action/sample.css");
        assertThat(Hereis.string()
                .trim(), IgnoringLS.equals(fileToString(cssFile03)
                .trim()));
        /*****************************************************
        #test06 {
            background-image: url("../img/sample.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }
        ****************************************************/

        File cssFile04 = new File(dumpDir, "action/sample/sample.css");
        assertThat(Hereis.string()
                .trim(), IgnoringLS.equals(fileToString(cssFile04)
                .trim()));
        /*****************************************************
        #test07 {
            background-image: url("../../img/sample.jpg");
            background-repeat: no-repeat;
            border-top: solid 0.06em;
            color: #333333;
            height: 1.8em;
            font-size: 4em;
            font-weight: bold;
            padding-left: 0.4em;
            z-index: 1;
        }
        ****************************************************/
    }

    /**
     * HttpRequestTestSupportのcreateHttpServerメソッドをオーバーライドして、MockのHttpServerを返却するクラス。
     *
     * @author Masato Inoue
     */
    private static class HttpRequestTestSupportExtends extends HttpRequestTestSupport {

        @Override
        protected HttpServer createHttpServer() {
            return new MockHttpServer();
        }

        @Override
        protected void prepareHandlerQueue(List<Handler> handlerQueue) {
        }
    }

    /**
     * HttpRequestTestSupportExtends.jsを無視するためクラス名を変えてオーバーライド
     *
     * @see HttpRequestTestSupport#copyHtmlResourceToDumpDir(HttpTestConfiguration, File, ResourceLocator)
     */
    private static class HttpRequestTestSupportExtends2 extends HttpRequestTestSupportExtends {

    }

    /**
     * HTTPServerのモック。
     */
    private static class MockHttpServer extends HttpServer {

        /**
         * handleメソッドでは何もしない。
         *
         * @param req HttpRequest
         * @param unused ExecutionContext
         */
        @Override
        public HttpResponse handle(HttpRequest req, ExecutionContext unused) {
            return new HttpResponse();
        }

        @Override
        public HttpServer start() {
            return this;
        }

        @Override
        public HttpServer join() {
            return this;
        }

        @Override
        public HttpServer startLocal() {
            return null;
        }
    }

    /**
     * HTMLチェッカーを置き換えて実行するテスト。
     */
    @Test
    public void testExecuteDummyHtmlChecker() throws Throwable {


        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-with-dummy-htmlcheck.xml");

        DummyHtmlChecker checker = repositoryResource.getComponent("htmlChecker");

        BasicHttpRequestTestTemplate target = new BasicHttpRequestTestTemplate(getClass()) {

            @Override
            protected String getBaseUri() {
                return "/action/DummyAction/";
            }

        };

        target.execute("testExecuteDummy");

        assertTrue(checker.isCalled());
    }

    /**
     * HTMLチェッカーを置き換えて実行するテスト。
     */
    @Test
    public void testExecuteExampleHtmlChecker() throws Throwable {


        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-with-example-htmlcheck.xml");

        BasicHttpRequestTestTemplate target = new BasicHttpRequestTestTemplate(getClass()) {

            @Override
            protected String getBaseUri() {
                return "/action/DummyAction/";
            }

        };

        target.execute("testExecute");
    }

    /**
     * HTMLチェッカーを置き換えて実行するテスト。
     */
    @Test
    public void testExecuteSimpleHtmlChecker() throws Throwable {


        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-with-simple-htmlcheck.xml");

        BasicHttpRequestTestTemplate target = new BasicHttpRequestTestTemplate(getClass()) {

            @Override
            protected String getBaseUri() {
                return "/action/DummyAction/";
            }

        };

        target.execute("testSimple");

    }

    /** {@link HttpRequestTestSupport#assertApplicationMessageId(String, String, nablarch.fw.ExecutionContext)}のテスト */
    @Test
    public void testAssertApplicationMessageId() {

        HttpRequestTestSupport target = new HttpRequestTestSupport();
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/assertMessageId.xml");

        // 正常にアサートできるパターン
        ExecutionContext ctx = new ExecutionContext();
        ApplicationException e = new ApplicationException();

        // エラーが発生していない場合
        target.assertApplicationMessageId("", ctx);
        target.assertApplicationMessageId(" ", ctx); // 空白はトリムされる。

        // メッセージが1件
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
        ctx.setRequestScopedVar("error", e);
        target.assertApplicationMessageId("MSG1", ctx);

        // エラーが複数の場合でもOK
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG2", new HashMap<String, String>())));
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG3", new HashMap<String, String>())));
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG4", new HashMap<String, String>())));
        ctx.setRequestScopedVar("error", e);
        target.assertApplicationMessageId("MSG1, MSG2, MSG4, MSG3", ctx);  // 期待値のカンマ区切りはスペースが混在してもOK

        // 同一メッセージがあってもOK
        e = new ApplicationException();
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
        e.addMessages(new Message(MessageLevel.INFO, new BasicStringResource("MSG3", new HashMap<String, String>())));
        e.addMessages(new Message(MessageLevel.ERROR, new BasicStringResource("MSG3", new HashMap<String, String>())));
        e.addMessages(new Message(MessageLevel.WARN, new BasicStringResource("MSG3", new HashMap<String, String>())));
        ctx.setRequestScopedVar("error", e);
        target.assertApplicationMessageId("MSG1 , MSG3 , MSG3 , MSG3", ctx);  // 期待値のカンマ区切りはスペースが混在してもOK
    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#assertApplicationMessageId(String[], nablarch.fw.ExecutionContext)}のエラー系テスト。 */
    @Test
    public void testAssertApplicationMessageIdError() {

        HttpRequestTestSupport target = new HttpRequestTestSupport();
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/assertMessageId.xml");

        try {
            // 期待値にメッセージを指定したのに、エラーが発生していない場合。
            ExecutionContext ctx = new ExecutionContext();
            target.assertApplicationMessageId(new String[] {"MSG1"}, ctx);
            fail("do not run.");
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("the request is normal end. message = []"));
        }

        try {
            // 期待値のサイズと実行結果のサイズが不一致
            ExecutionContext ctx = new ExecutionContext();
            ApplicationException e = new ApplicationException();
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
            ctx.setRequestScopedVar("error", e);

            target.assertApplicationMessageId("case:1", new String[] {}, ctx);
            fail("do not run.");
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(),
                    is("case:1 expected:<[[]]> but was:<[[MSG1]]>"));
        }

        try {
            // 期待値のサイズと実行結果のサイズが不一致
            ExecutionContext ctx = new ExecutionContext();
            ApplicationException e = new ApplicationException();
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
            ctx.setRequestScopedVar("error", e);

            target.assertApplicationMessageId("case:2", new String[] {"MSG1", "MSG2"}, ctx);
            fail("do not run.");
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(),
                    is("case:2 expected:<[MSG1[, MSG2]]> but was:<[MSG1[]]>"));
        }

        try {
            // メッセージIDがの不一致
            ExecutionContext ctx = new ExecutionContext();
            ApplicationException e = new ApplicationException();
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
            ctx.setRequestScopedVar("error", e);

            target.assertApplicationMessageId("case:2", new String[] {"MSG2"}, ctx);
            fail("do not run.");
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(),
                    is("case:2 different element(s) found. expected has [MSG2], actual has [MSG1].  "
                            + "expected:<[MSG[2]]> but was:<[MSG[1]]>"));

        }

        try {
            // メッセージIDがの不一致その２
            ExecutionContext ctx = new ExecutionContext();
            ApplicationException e = new ApplicationException();
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG1", new HashMap<String, String>())));
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG2", new HashMap<String, String>())));
            e.addMessages(
                    new Message(MessageLevel.ERROR, new BasicStringResource("MSG2", new HashMap<String, String>())));
            ctx.setRequestScopedVar("error", e);

            target.assertApplicationMessageId("メッセージID不一致のケースその２", new String[] {"MSG1", "MSG2", "MSG1"}, ctx);
            fail("do not run.");
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(),
                    is("メッセージID不一致のケースその２ different element(s) found. "
                            + "expected has [MSG1], actual has [MSG2].  "
                            + "expected:<[MSG1, MSG2, MSG[1]]> but was:<[MSG1, MSG2, MSG[2]]>"));
        }

    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#setValidToken(nablarch.fw.web.HttpRequest, ExecutionContext)}のテスト。 */
    @Test
    public void testSetValidToken() {

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        MockHttpRequest request = new MockHttpRequest();
        ExecutionContext context = new ExecutionContext();

        target.setValidToken(request, context);

        WebConfig webConfig = WebConfigFinder.getWebConfig();

        String[] paramToken = request.getParam(webConfig.getDoubleSubmissionTokenParameterName());
        assertThat(paramToken.length, is(1));

        Object sessionToken = context.getSessionScopedVar(webConfig.getDoubleSubmissionTokenSessionAttributeName());
        assertTrue(sessionToken != null);

        assertTrue(paramToken[0].equals(sessionToken));
    }

    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#setToken(nablarch.fw.web.HttpRequest, nablarch.fw.ExecutionContext, boolean)}のテスト。
     * <p/>
     * validなTokenを発行するケースは、setValidTokenのテストで実施されているため、invalidなTokenを発行するケースのみ実施する。
     */
    @Test
    public void testSetToken() throws Exception {
        HttpRequestTestSupport target = new HttpRequestTestSupport();

        HttpRequest request = new MockHttpRequest();
        ExecutionContext context = new ExecutionContext();

        target.setToken(request, context, false);

        WebConfig webConfig = WebConfigFinder.getWebConfig();

        String[] paramToken = request.getParam(webConfig.getDoubleSubmissionTokenParameterName());
        assertThat("リクエストパラメータにTokenが含まれないこと。", paramToken, is(nullValue()));

        Object sessionToken = context.getSessionScopedVar(webConfig.getDoubleSubmissionTokenSessionAttributeName());
        assertThat("セッションにTokenが含まれないこと。", sessionToken, is(nullValue()));
    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#checkHtml(String, HttpTestConfiguration)} */
    @Test
    public void testCheckHtml() {
        HttpRequestTestSupport target = new HttpRequestTestSupport();

        HttpTestConfiguration config = new HttpTestConfiguration();

        // 設定ファイルの指定が誤っている場合
        try {
            config.setHtmlCheckerConfig("resources/no-such-config.csv");
            target.checkHtml("resources/nablarch/test/core/http/testCheckNormal.html",
                    config);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertEquals("can't read file [resources/no-such-config.csv]", e.getMessage());
        }

        config.setHtmlCheckerConfig("src/test/resources/html-check-config.csv");
        // Htmlファイルに指摘がない場合
        target.checkHtml("src/test/resources/nablarch/test/core/http/testCheckNormal.html",
                config);

        // Htmlファイルに指摘がある場合
        try {
            config.setHtmlCheckerConfig("src/test/resources/html-check-config.csv");
            target.checkHtml("src/test/resources/nablarch/test/core/http/testCheckForbidden.html",
                    config);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidHtmlException);
        }

        // HTMLファイルの指定が誤っている場合
        try {
            config.setHtmlCheckerConfig("src/test/resources/html-check-config.csv");
            target.checkHtml("src/test/resources/nablarch/test/core/http/noSuchHtml.html",
                    config);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidHtmlException);
        }

        MockHtmlChecker mockChecker = new MockHtmlChecker();
        config.setHtmlChecker(mockChecker);
        target.checkHtml("src/test/resources/nablarch/test/core/http/noSuchHtml.html",
                config);

        assertEquals(new File("src/test/resources/nablarch/test/core/http/noSuchHtml.html"), mockChecker.html);

    }

    private static class MockHtmlChecker implements HtmlChecker {

        private File html = null;

        @Override
        public void checkHtml(File html) {
            this.html = html;
        }
    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#assertObjectPropertyEquals(String, String, String, Object)} */
    @Test
    public void testAssertObjectPropertyEquals() {
        HttpRequestTestSupport target = new HttpRequestTestSupport(getClass());
        {
            // テスト対象のプロパティにセットされた値が正しい場合。
            ValidationTargetBean bean = new ValidationTargetBean();
            bean.setStrVal("テスト");
            bean.setIntVal(101);
            target.assertObjectPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanProps", bean);
        }

        {
            // テスト対象のプロパティにセットされた値が正しくない場合。
            ValidationTargetBean bean = new ValidationTargetBean();
            bean.setStrVal("テス");
            bean.setIntVal(101);
            try {
                target.assertObjectPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanProps", bean);
                fail("例外が発生するはず。");
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("strVal=テスト"));
                assertThat(expected, containsString("intVal=101"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("strVal=テス"));
                assertThat(actual, containsString("intVal=101"));
                // メッセージが入っている
                assertThat(message, containsString("test message"));
            }
        }
        {
            // 別シート、別IDでアサートできることを確認。
            ValidationTargetBean bean = new ValidationTargetBean();
            bean.setStrVal("テス");
            bean.setIntVal(101);

            // 前のテストと同じデータだが、シートとIDが違うのでエラーにならない
            target.assertObjectPropertyEquals("test message", "testAssertObjectPropertyEquals2", "beanProps1", bean);
        }
        {
            // 期待値のリストが取得できない場合、例外が発生し、メッセージには指定されたシート名とLIST_MAP名が含まれていることを確認する。
            try {
                target.assertObjectPropertyEquals("test message", "testAssertObjectPropertyEquals2", "nonExisting",
                        new ValidationTargetBean());
                fail("例外が発生するはず。");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage(), containsString("testAssertObjectPropertyEquals2"));
                assertThat(e.getMessage(), containsString("nonExisting"));
            }
        }
    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#assertObjectArrayPropertyEquals(String, String, String, Object[]) */
    @Test
    public void testAssertObjectArrayPropertyEquals() {

        HttpRequestTestSupport target = new HttpRequestTestSupport(getClass());


        {
            // テスト対象のプロパティにセットされた値が正しい場合。
            ValidationTargetBean[] beans = new ValidationTargetBean[3];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new ValidationTargetBean();
                beans[i].setStrVal("テスト" + i);
                beans[i].setIntVal(100 + i);
            }
            target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanArrayProps",
                    beans);
        }

        {
            // actual が nullで予想結果が空の場合。
            target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals", "nullValue", null);
        }

        {
            // actual が 空配列で予想結果が空の場合。
            target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals", "nullValue",
                    new ValidationTargetBean[0]);
        }
        {
            // テスト対象のプロパティにセットされた値が正しくない場合。
            ValidationTargetBean[] beans = new ValidationTargetBean[3];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new ValidationTargetBean();
                beans[i].setStrVal("テスト" + i);
                beans[i].setIntVal(100 + i);
            }
            beans[1].setStrVal("テス1");
            try {
                target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals",
                        "beanArrayProps", beans);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("strVal=テスト1"));
                assertThat(expected, containsString("intVal=101"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("strVal=テス1"));
                assertThat(actual, containsString("intVal=101"));
                // 行番号が入っている
                assertThat(message, containsString("target index [1]"));
                // メッセージが入っている
                assertThat(message, containsString("test message"));
            }
        }

        {
            // 別シート、別IDでアサートできることを確認。

            // テスト対象のプロパティにセットされた値が正しい場合。
            ValidationTargetBean[] beans = new ValidationTargetBean[3];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new ValidationTargetBean();
                beans[i].setStrVal("テスト" + i);
                beans[i].setIntVal(100 + i);
            }
            beans[1].setStrVal("テス1");
            // 前のテストと同じデータだが、シートとIDが違うのでエラーにならない
            target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals2", "beanArrayProps1",
                    beans);
        }

        {
            // actualがnullの場合。
            try {
                target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals",
                        "beanArrayProps", null);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("3"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("null"));
                // 行番号が入っている
                assertThat(message, containsString("target size does not match"));
            }
        }

        {
            // 配列長が合わない場合。
            ValidationTargetBean[] beans = new ValidationTargetBean[2];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new ValidationTargetBean();
                beans[i].setStrVal("テスト" + i);
                beans[i].setIntVal(100 + i);
            }
            beans[1].setStrVal("テス1");
            try {
                target.assertObjectArrayPropertyEquals("test message", "testAssertObjectPropertyEquals",
                        "beanArrayProps", beans);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("3"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("2"));
                // 行番号が入っている
                assertThat(message, containsString("target size does not match"));
            }
        }
    }

    /** {@link nablarch.test.core.http.HttpRequestTestSupport#assertObjectListPropertyEquals(String, String, String, List) */
    @Test
    public void testAssertObjectListPropertyEquals() {

        HttpRequestTestSupport target = new HttpRequestTestSupport(getClass());


        {
            // テスト対象のプロパティにセットされた値が正しい場合。
            List<ValidationTargetBean> beans = new ArrayList<ValidationTargetBean>();
            for (int i = 0; i < 3; i++) {
                ValidationTargetBean bean = new ValidationTargetBean();
                bean.setStrVal("テスト" + i);
                bean.setIntVal(100 + i);
                beans.add(bean);
            }
            target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanListProps",
                    beans);
        }


        {
            // actual が nullで予想結果が空の場合。
            target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "nullValue", null);
        }

        {
            // actual が 空リストで予想結果が空の場合。
            target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "nullValue",
                    new ArrayList<ValidationTargetBean>());
        }
        {
            // テスト対象のプロパティにセットされた値が正しい場合。
            List<ValidationTargetBean> beans = new ArrayList<ValidationTargetBean>();
            for (int i = 0; i < 3; i++) {
                ValidationTargetBean bean = new ValidationTargetBean();
                bean.setStrVal("テスト" + i);
                bean.setIntVal(100 + i);
                beans.add(bean);
            }
            beans.get(1)
                    .setStrVal("テス1");
            try {
                target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanListProps",
                        beans);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("strVal=テスト1"));
                assertThat(expected, containsString("intVal=101"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("strVal=テス1"));
                assertThat(actual, containsString("intVal=101"));
                // 行番号が入っている
                assertThat(message, containsString("target index [1]"));
                // メッセージが入っている
                assertThat(message, containsString("test message"));
            }
        }

        {
            // リスト長が合わない場合。

            List<ValidationTargetBean> beans = new ArrayList<ValidationTargetBean>();
            for (int i = 0; i < 2; i++) {
                ValidationTargetBean bean = new ValidationTargetBean();
                bean.setStrVal("テスト" + i);
                bean.setIntVal(100 + i);
                beans.add(bean);
            }
            beans.get(1)
                    .setStrVal("テス1");
            try {
                target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanListProps",
                        beans);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("3"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("2"));
                // 行番号が入っている
                assertThat(message, containsString("target size does not match"));
            }
        }

        {
            // 引数が null の場合。

            try {
                target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals", "beanListProps",
                        null);
                fail();
            } catch (ComparisonFailure error) {
                String actual = error.getActual();
                String expected = error.getExpected();
                String message = error.getMessage();

                // 予想結果のプロパティ名が入っている
                assertThat(expected, containsString("3"));
                // 現在値のプロパティ名が入っている
                assertThat(actual, containsString("null"));
                // 行番号が入っている
                assertThat(message, containsString("target size does not match"));
            }
        }

        {
            // 別シート、別IDでアサートできることを確認。

            // テスト対象のプロパティにセットされた値が正しい場合。
            List<ValidationTargetBean> beans = new ArrayList<ValidationTargetBean>();
            for (int i = 0; i < 3; i++) {
                ValidationTargetBean bean = new ValidationTargetBean();
                bean.setStrVal("テスト" + i);
                bean.setIntVal(100 + i);
                beans.add(bean);
            }
            beans.get(1)
                    .setStrVal("テス1");
            // 前のテストと同じデータだが、シートとIDが違うのでエラーにならない
            target.assertObjectListPropertyEquals("test message", "testAssertObjectPropertyEquals2", "beanListProps1",
                    beans);
        }
    }

    /**
     * {@link HttpRequestTestSupport#createHttpRequest(String, Map)} のテスト。
     */
    @Test
    public void testCreateHttpRequest() {

        repositoryResource.addComponent("httpTestConfiguration", new HttpTestConfiguration());

        HttpRequestTestSupport target = new HttpRequestTestSupport(getClass());

        // HTTPパラメータの元ネタを作成（テストデータで書いたもの）
        Map<String, String[]> params = new HashMap<String, String[]>();
        // 添付ファイル
        params.put("upload_file", new String[] {"${attach:src/test/java/MASTER_DATA.xls}", "${attach:src/test/resources/unit-test.config}"});
        // 添付ファイル以外のパラメータ
        params.put("hoge", new String[] {"fuga1", "fuga2"});

        // 実行
        HttpRequest request = target.createHttpRequest("requestUri", params);

        // 結果確認
        // ２つのファイルがアップロードされていること
        List<PartInfo> uploadFiles = request.getPart("upload_file");
        assertThat(uploadFiles.size(), is(2));

        // １つめのファイル
        {
            PartInfo partInfo = uploadFiles.get(0);
            assertThat(partInfo.getFileName(), is("MASTER_DATA.xls")); // ファイル名が抽出されていること
            assertThat(partInfo.size(), is(not(0)));                     // ファイルサイズが設定されていること
        }

        {
            PartInfo partInfo = uploadFiles.get(1);
            assertThat(partInfo.getFileName(), is("unit-test.config"));       // ファイル名が抽出されていること
            assertThat(partInfo.size(), is(not(0)));                     // ファイルサイズが設定されていること
        }

        // HTTPパラメータにアップロードファイルのファイル名が含まれていること
        String[] uploadFileParams = params.get("upload_file");
        assertThat(uploadFileParams.length, is(2));
        assertThat(uploadFileParams[0], is("MASTER_DATA.xls"));
        assertThat(uploadFileParams[1], is("unit-test.config"));

        // アップロード以外のパラメータが設定されていること
        String[] hoge = params.get("hoge");
        assertThat(hoge.length, is(2));
        assertThat(hoge[0], is("fuga1"));
        assertThat(hoge[1], is("fuga2"));
    }


    /**
     * {@link HttpRequestTestSupport#assertTableEquals(String)} で
     * グループIDにマッチするテーブルデータが１つも存在しない場合、例外が発生すること。
     */
    @Test
    public void testAssertTableEqualsWithInvalidGroupId() {
        final HttpRequestTestSupport target = new HttpRequestTestSupport(getClass());
        try {
            // シートは存在するが、EXPECTED_TABLEは存在しない。
            target.assertTableEquals("testAssertObjectPropertyEquals");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith(" no table data found in the specified sheet."));
            assertThat(e.getMessage(), containsString("sheet=[testAssertObjectPropertyEquals"));
            assertThat(e.getMessage(), containsString("groupId=[]"));
        }
    }

    /**
     * オブジェクトのバリデーションサンプルのBean
     *
     * @author Koichi Asano
     */
    public static class ValidationTargetBean {

        private String strVal;

        private int intVal;

        public String getStrVal() {
            return strVal;
        }

        public void setStrVal(String strVal) {
            this.strVal = strVal;
        }

        public int getIntVal() {
            return intVal;
        }

        public void setIntVal(int intVal) {
            this.intVal = intVal;
        }
    }

    /**
     * {@link nablarch.test.core.http.HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}のテスト。
     * 正常系。ExtensionListが空のパターン。
     */
    @Test
    public void testExecuteNoBackup() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/noBackup.xml");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends");
        File webDir = new File("src/test/resources/nablarch/test/core/http/web");

        dumpDir.mkdirs();
        assertTrue(dumpDir.exists());
        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();

        // 最終更新が古いjavascriptファイルをダンプ先ディレクトリに配置
        File jsfile = new File(dumpDir, "js/sample.js");
        if (!jsfile.getParentFile()
                .exists()) {
            jsfile.getParentFile()
                    .mkdirs();
        }
        jsfile.createNewFile();
        jsfile.setLastModified(new File(webDir, "js/sample.js").lastModified() - 1);

        // コピー元に存在しないJSファイルをダンプ先ディレクトリに配置
        new File(dumpDir, "js/sample_tmp.js").createNewFile();
        // コピー元に存在しないHTMLファイルをダンプ先ディレクトリに配置
        new File(dumpDir, "test.html").createNewFile();


        target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());

        // cssがコピーされることの確認
        String msg = dumpDir + "にリソースがコピーされているはず";
        assertTrue(msg, new File(dumpDir, "css/sample01.css").exists());
        assertTrue(msg, new File(dumpDir, "css/sample02.css").exists());
        assertTrue(msg, new File(dumpDir, "action/sample.css").exists());
        assertTrue(msg, new File(dumpDir, "action/sample/sample.css").exists());
        // cssがコピーされることの確認
        assertTrue(msg, new File(dumpDir, "img/sample01.jpg").exists());
        assertTrue(msg, new File(dumpDir, "img/sample02.jpg").exists());
        // JSが存在することを確認
        assertTrue(msg, new File(dumpDir, "js/sample.js").exists());
        // 上書きされていることを確認
        assertThat(
                "タイムスタンプが異なるので上書きコピーされること",
                new File(dumpDir, "js/sample.js").lastModified(),
                is(new File(webDir, "js/sample.js").lastModified())
        );
        assertFalse("コピー元に無いリソースファイルは削除される。", new File(dumpDir, "js/sample_tmp.js").exists());
        assertTrue("ただしHTMLファイルは(直前のテストの結果なので)削除しない", new File(dumpDir, "test.html").exists());

        // バックアップが作成されていないことの確認
        msg = destDir + "/test_dump_bk が作成されていないはず";
        assertFalse(msg, new File(destDir, "test_dump_bk").exists());
    }

    @Test
    public void testErrorHandlingWhenImproperlyConfigured() {
        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();
        try {
            target.checkHtml("dummy", new HttpTestConfiguration());
            fail();
        } catch (Exception e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertTrue(e.getMessage()
                    .contains("HtmlChecker not found."));
        }
    }

    /**
     * {@link HttpRequestTestSupport#setHttpHeader(nablarch.fw.web.HttpRequest, HttpTestConfiguration)}のテスト。
     */
    @Test
    public void testSetHttpHeader() {
        {
            MockHttpRequest request = new MockHttpRequest();
            Map<String, String> oldHeader = new HashMap<String, String>(1);
            oldHeader.put("key1", "value1");
            request.setHeaderMap(oldHeader);

            HttpTestConfiguration conf = new HttpTestConfiguration();
            Map<String, String> newHeader = new HashMap<String, String>(1);
            newHeader.put("key2", "value2");
            conf.setHttpHeader(newHeader);

            HttpRequestTestSupport.setHttpHeader(request, conf);
            assertThat("もともとHttpRequestに設定されていた値は保持されていること。", request.getHeader("key1"), is("value1"));
            assertThat("テスト用設定の値もヘッダにマージされていること。", request.getHeader("key2"), is("value2"));
        }
        {
            MockHttpRequest request = new MockHttpRequest();
            Map<String, String> oldHeader = new HashMap<String, String>(1);
            oldHeader.put("duplicate_key", "old");
            request.setHeaderMap(oldHeader);

            HttpTestConfiguration conf = new HttpTestConfiguration();
            Map<String, String> newHeader = new HashMap<String, String>(1);
            newHeader.put("duplicate_key", "new");
            conf.setHttpHeader(newHeader);

            HttpRequestTestSupport.setHttpHeader(request, conf);
            assertThat("既にヘッダに同じキーで値が設定されている場合には、上書きしないこと。", request.getHeader("duplicate_key"), is("old"));
        }
    }

    /**
     * {@link HttpRequestTestSupport#deleteHtmlResourceFile(File, File)}のテスト。
     */
    @Test
    public void testDeleteHtmlResourceFile() {
        HttpRequestTestSupport target = new HttpRequestTestSupport();
        // コピー先ディレクトリが指定されなかった場合は正常に終了する。
        target.deleteHtmlResourceFile(null, new File("notADirectory"));
    }

    /**
     * {@link HttpRequestTestSupport#prepareHandlerQueue(List)}のテスト。
     */
    @Test
    public void testPrepareHandlerQueue() {
        SessionConcurrentAccessHandler.class.getPackage();

        // フィクスチャの準備
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testPrepareHandlerQueue.xml");
        HttpRequestTestSupport target = new HttpRequestTestSupport();

        // テスト実行
        target.execute("testPrepareHandlerQueue", new MockHttpRequest().setRequestUri("/action/DummyAction/ForwardJSP"),
                new ExecutionContext());

        SessionConcurrentAccessHandler sessionHandler = repositoryResource.getComponent("sessionHandler");
        assertThat("SessionConcurrentAccessHandlerのconcurrentAccessPolisyがCONCURRENTに設定されていること。",
                sessionHandler.getConcurrentAccessPolicy(), is(ConcurrentAccessPolicy.CONCURRENT));
    }

    /**
     * {@link HttpRequestTestSupport#copyHtmlResources(HttpTestConfiguration, File)}のテスト。
     */
    @Test
    public void testSkipResourceCopy_DirAlreadyExists() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testExecute03.xml");

        File dumpDir = new File(destDir, "test_dump/HttpRequestTestSupportExtends");
        dumpDir.mkdirs();
        assertTrue(dumpDir.exists());
        File resourceDir = new File("tmp/test_dump", "../testHtmlResourcesRoot");
        resourceDir.mkdirs();
        assertTrue(resourceDir.exists());

        System.setProperty("nablarch.test.skip-resource-copy", "true");

        HttpRequestTestSupport target = new HttpRequestTestSupportExtends();
        try {
            target.execute("testExecute", new MockHttpRequest(), new ExecutionContext());
        } finally {
            System.clearProperty("nablarch.test.skip-resource-copy");
        }
        // cssがコピーされないことの確認
        assertFalse(new File(dumpDir, "css/sample01.css").exists());
        assertFalse(new File(dumpDir, "css/sample02.css").exists());
        assertFalse(new File(dumpDir, "action/sample.css").exists());
        assertFalse(new File(dumpDir, "action/sample/sample.css").exists());
    }

    /**
     * {@link DbAccessTestSupport}に処理を委譲しているメソッドのテスト
     */
    @Test
    public void testDelegatingToDbSupport() {

        VariousDbTestHelper.createTable(HttpTestSupportTable.class);

        HttpRequestTestSupport target = new HttpRequestTestSupport(HttpRequestTestSupportTest.class);

        // DbAccessTestSupportに委譲しているもの
        // テーブルアサート
        target.setUpDb("testDelegatingToDbSupport", "1");
        target.assertTableEquals("testDelegatingToDbSupport", "1");
        target.assertTableEquals("testDelegatingToDbSupport");
        target.assertTableEquals("メッセージ付きのやつ", "testDelegatingToDbSupport", "1");

        SqlResultSet resultSet = new SimpleDbTransactionExecutor<SqlResultSet>(
                (SimpleDbTransactionManager) repositoryResource
                        .getComponent(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST)) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("SELECT * FROM HTTP_TEST_SUPPORT_TABLE");
                return statement.retrieve();
            }
        }.doTransaction();

        // SQL実行結果のアサート
        target.assertSqlResultSetEquals("SqlResultSetEquals", "testDelegatingToDbSupport", "resultSetEquals",
                resultSet);
        target.assertSqlRowEquals("SqlResultSetEquals", "testDelegatingToDbSupport", "rowEquals", resultSet.get(0));


        // ListParamMapの取得
        List<Map<String, String[]>> listParamMap = target.getListParamMap("testDelegatingToDbSupport",
                "resultSetEquals");
        assertArrayEquals(new String[] {"0"}, listParamMap.get(0)
                .get("PK_COL1"));

        // ParamMapの取得
        assertArrayEquals(new String[] {"1"}, listParamMap.get(0)
                .get("PK_COL2"));
        Map<String, String[]> paramMap = target.getParamMap("testDelegatingToDbSupport", "resultSetEquals");
        assertArrayEquals(new String[] {"0"}, paramMap.get("PK_COL1"));
        assertArrayEquals(new String[] {"1"}, paramMap.get("PK_COL2"));
    }

    /**
     * {@link nablarch.test.core.db.EntityTestSupport}に処理を委譲しているメソッドのテスト
     */
    @Test
    public void testDelegatingToEntitySupport() {
        HttpRequestTestSupport target = new HttpRequestTestSupport(HttpRequestTestSupportTest.class);

        TestEntity actual = new TestEntity();
        actual.setProp1("prop1");
        actual.setProp2("prop2");
        target.assertEntity("testDelegatingToEntitySupport", "entity", actual);
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder anotherTemporaryFolder = new TemporaryFolder();

    @Test
    public void testRewriteResourceFile() throws Exception {
        File tempFolder = temporaryFolder.getRoot();
        File jsTestResourcePath = new File(tempFolder, "jsTestResourcePath");
        jsTestResourcePath.mkdir();

        File cssFile = new File(jsTestResourcePath, "hoge.css");
        new FileWriter(cssFile).append('a')
                .close();

        File jsFile = new File(jsTestResourcePath, "hoge.js");
        new FileWriter(jsFile).append(
                "contextPath {contextPath}/hoge.png contextPath}\n {contextPath {contextPath}/fuga.png contextpath")
                .close();

        File templateFile = new File(jsTestResourcePath, "hoge.template");
        new FileWriter(templateFile).append(
                "{contextPath}/{filename}.png {contextPath}{contextPath}{contextPath }contextPath}")
                .close();

        File warBasePath = new File(tempFolder, "warBasePath");
        warBasePath.mkdir();

        File dumpDir = new File(tempFolder, "dumpDir");
        dumpDir.mkdir();
        File expectedCssFile = new File(dumpDir, "hoge.css");
        assertThat("テスト実行前なのでCSSはダンプディレクトリには存在しない", expectedCssFile.exists(), is(false));

        File expectedJsFile = new File(dumpDir, "hoge.js");
        assertThat("テスト実行前なのでjsはダンプディレクトリには存在しない", expectedJsFile.exists(), is(false));

        File expectedTemplateFile = new File(dumpDir, "hoge.template");
        assertThat("テスト実行前なのでtemplateはダンプディレクトリには存在しない", expectedTemplateFile.exists(), is(false));

        // setup
        HttpRequestTestSupport.jsTestResourcePath = null;
        HttpTestConfiguration configuration = new HttpTestConfiguration();
        configuration.setJsTestResourceDir(jsTestResourcePath.getAbsolutePath());

        HttpRequestTestSupport sut = new HttpRequestTestSupport();
        FileFilter filter = sut.getFileFilter(configuration);
        jsTestResourcePath.listFiles(filter);

        sut.rewriteResourceFile(configuration, dumpDir,
                ResourceLocator.valueOf("file://" + NablarchTestUtils.toCanonicalPath(warBasePath.getAbsolutePath())));

        assertThat("CSSはダンプディレクトリにコピーされる", expectedCssFile.exists(), is(true));
        assertThat("jsファイルはダンプディレクトリにコピーされる", expectedJsFile.exists(), is(true));
        assertThat("templateファイルはダンプディレクトリにコピーされる", expectedTemplateFile.exists(), is(true));

        String replacedJs = readFile(expectedJsFile);
        assertThat("{contextPath}が「.」に置き換えられること", replacedJs, is(
                "contextPath ./hoge.png contextPath}\n {contextPath ./fuga.png contextpath\n"));
        String replacedTemplate = readFile(expectedTemplateFile);
        assertThat("{contextPath}が「.」に置き換えられること", replacedTemplate, is(
                "./{filename}.png ..{contextPath }contextPath}\n"));


        // タイムスタンプが同じ場合にはコピーされないこと
        File anotherTempFolder = anotherTemporaryFolder.getRoot();
        jsTestResourcePath = new File(anotherTempFolder, "jsTestResourcePath");
        jsTestResourcePath.mkdir();

        File anotherCssFile = new File(jsTestResourcePath, "hoge.css");
        new FileWriter(anotherCssFile).append("abcde")
                .close();
        anotherCssFile.setLastModified(cssFile.lastModified());

        warBasePath = new File(anotherTempFolder, "warBasePath");
        warBasePath.mkdir();

        configuration = new HttpTestConfiguration();
        configuration.setJsTestResourceDir(jsTestResourcePath.getAbsolutePath());
        sut = new HttpRequestTestSupport();
        filter = sut.getFileFilter(configuration);
        jsTestResourcePath.listFiles(filter);

        HttpRequestTestSupport.jsTestResourcePath = null;
        sut.rewriteResourceFile(configuration, dumpDir,
                ResourceLocator.valueOf("file://" + NablarchTestUtils.toCanonicalPath(warBasePath.getAbsolutePath())));

        assertThat(expectedCssFile.length(), is(not(anotherCssFile.length())));
        // PrintWriter#printlnで書きだされているので、コピー後のCSSファイルには改行が入っている。
        String separator = System.getProperty("line.separator") == null ? "\n" : System.getProperty("line.separator");
        assertThat(expectedCssFile.length(), is(cssFile.length() + separator.length()));
    }

    /**
     * 【合格条件】<br/>
     * テストクラスで生成したセッション情報が、業務Actionで参照できること<br/>
     * 業務Actionで上書きしたセッション情報が、テストクラスで検証できること。<br/>
     * ※SessionStoreにhttpSessionを利用する
     *
     * @throws Exception
     */
    @Test
    public void testSessionCopyAndChange() throws Exception {

        // パッケージマッピングとSessionStoreを有効化するために必要なハンドラを設定
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testSessionCopyAndChange.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        ExecutionContext ctx = new ExecutionContext();

        // 上書き対象のStringを格納
        ctx.setRequestScopedVar("requestScope", "requestScope_value");
        ctx.setSessionScopedVar("sessionScope", "sessionScope_value");
        SessionUtil.put(ctx,"sessionStore", "sessionStore_value");

        // コピーに失敗している場合は実行時エラーが送出される
        target.execute("testSessionCopyAndChange",
                new MockHttpRequest().setRequestUri("/action/ContextCopyTestAction/copyAndChangeValue"),
                ctx);

        // 業務Actionによって値が上書きされたことを確認する
        assertEquals("requestScope_value_change", ctx.getRequestScopedVar("requestScope"));
        assertEquals("sessionScope_value_change", ctx.getSessionScopedVar("sessionScope"));
        assertEquals("sessionStore_value_change", SessionUtil.get(ctx,"sessionStore"));
    }

    /**
     * 【合格条件】<br/>
     * 業務Actionでセッション情報を削除したことがテストクラスで検証できること。<br/>
     * ※SessionStoreにhttpSessionを利用する
     *
     * @throws Exception
     */
    @Test
    public void testSessionDelete() throws Exception {

        // パッケージマッピングとSessionStoreを有効化するために必要なハンドラを設定
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testSessionDelete.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        ExecutionContext ctx = new ExecutionContext();

        // 削除対象のStringを格納
        ctx.setRequestScopedVar("requestScope_removeTarget", "requestScope_value_remove");
        ctx.setSessionScopedVar("sessionScope_removeTarget", "sessionScope_value_remove");
        SessionUtil.put(ctx,"sessionStore_removeTarget", "sessionStore_value_remove");

        // コピーに失敗している場合は実行時エラーが送出される
        target.execute("sessionDelete",
                new MockHttpRequest().setRequestUri("/action/ContextCopyTestAction/sessionDelete"),
                ctx);

        assertNull(ctx.getRequestScopedVar("requestScope_removeTarget"));
        assertNull(ctx.getSessionScopedVar("sessionScope_removeTarget"));
        assertNull(SessionUtil.orNull(ctx,"sessionStore_removeTarget"));
    }

    /**
     * 【合格条件】<br/>
     * 業務Actionでセッション情報を破棄したことがテストクラスで検証できること。<br/>
     * ※SessionStoreにhttpSessionを利用する
     *
     * @throws Exception
     */
    @Test
    public void sessionInvalidate() throws Exception {

        // パッケージマッピングとSessionStoreを有効化するために必要なハンドラを設定
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testSessionInvalidate.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        ExecutionContext ctx = new ExecutionContext();

        ctx.setSessionScopedVar("sessionScope", "sessionScope_value");
        SessionUtil.put(ctx,"sessionStore", "sessionStore_value");

        // コピーに失敗している場合は実行時エラーが送出される
        target.execute("sessionInvalidate",
                new MockHttpRequest().setRequestUri("/action/ContextCopyTestAction/invalidateSession"),
                ctx);

        // SessionScopeが破棄されたcontextを保持していることを確認する
        assertNull(SessionUtil.orNull(ctx,"sessionStore"));
        assertEquals(0,ctx.getSessionScopeMap().size());

        // SessionStoreが破棄されたcontextを保持していることを確認する
        assertNull(ctx.getSessionScopedVar("sessionScope"));
        assertNotNull(ctx.getSessionStoreMap().get(SessionStoreHandler.IS_INVALIDATED_KEY));
    }

    /**
     * 【合格条件】<br/>
     * DbStoreが利用可能であること。
     *
     * @throws Exception
     */
    @Test
    public void testAvailableDbStore() throws Exception {

        // パッケージマッピングとSessionStoreを有効化するために必要なハンドラを設定
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testAvailableDbStore.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        //DBStoreの設定
        DbStore store = repositoryResource.getComponent("dbStore");
        store.initialize();

        ExecutionContext ctx = new ExecutionContext();

        // 上書き対象のStringを格納
        ctx.setRequestScopedVar("requestScope", "requestScope_value");
        ctx.setSessionScopedVar("sessionScope", "sessionScope_value");
        SessionUtil.put(ctx,"sessionStore", "sessionStore_value");

        // コピーに失敗している場合は実行時エラーが送出される
        target.execute("testAvailableDbSore",
                new MockHttpRequest().setRequestUri("/action/ContextCopyTestAction/copyAndChangeValue"),
                ctx);

        // 業務Actionによって値が上書きされたことを確認する
        assertEquals("requestScope_value_change", ctx.getRequestScopedVar("requestScope"));
        assertEquals("sessionScope_value_change", ctx.getSessionScopedVar("sessionScope"));
        assertEquals("sessionStore_value_change", SessionUtil.get(ctx,"sessionStore"));

    }

    /**
     * 【合格条件】<br/>
     * hiddenStoreが利用可能であること。
     *
     * @throws Exception
     */
    @Test
    public void testAvailableHiddenStore() throws Exception {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/http/testAvailableHiddenStore.xml");

        HttpRequestTestSupport target = new HttpRequestTestSupport();

        ExecutionContext ctx = new ExecutionContext();

        // 上書き対象のStringを格納
        ctx.setRequestScopedVar("requestScope", "requestScope_value");
        ctx.setSessionScopedVar("sessionScope", "sessionScope_value");
        SessionUtil.put(ctx,"sessionStore", "sessionStore_value");

        // コピーに失敗している場合は実行時エラーが送出される
        target.execute("testAvailableHiddenStore",
                new MockHttpRequest().setRequestUri("/action/ContextCopyTestAction/copyAndChangeValue"),
                ctx);

        // 業務Actionによって値が上書きされたことを確認する
        assertEquals("requestScope_value_change", ctx.getRequestScopedVar("requestScope"));
        assertEquals("sessionScope_value_change", ctx.getSessionScopedVar("sessionScope"));
        assertEquals("sessionStore_value_change", SessionUtil.get(ctx,"sessionStore"));

    }

    private String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            result.append(line);
            result.append('\n');
        }
        reader.close();
        return result.toString();
    }

    @Entity
    @Table(name = "HTTP_TEST_SUPPORT_TABLE")
    public static class HttpTestSupportTable {

        public HttpTestSupportTable() {
        }

        ;

        public HttpTestSupportTable(String pkCol1, Long pkCol2) {
            this.pkCol1 = pkCol1;
            this.pkCol2 = pkCol2;
        }

        @Id
        @Column(name = "PK_COL1", length = 1, nullable = false)
        public String pkCol1;

        @Column(name = "PK_COL2", length = 2)
        public Long pkCol2;
    }

    /**
     * ユーザセッションテーブル
     *
     */
    @Entity
    @Table(name = "USER_SESSION")
    public class UserSession {

        public UserSession() {
        };

        public UserSession(String sessionId, byte[] sessionObject, Timestamp expirationDatetime) {
            this.sessionId = sessionId;
            this.sessionObject = sessionObject;
            this.expirationDatetime = expirationDatetime;
        }

        @Id
        @Column(name = "SESSION_ID", nullable = false)
        public String sessionId;

        @Lob
        @Column(name = "SESSION_OBJECT")
        public byte[] sessionObject;

        @Column(name = "EXPIRATION_DATETIME")
        public Timestamp expirationDatetime;
    }

    public static class TestEntity {

        private String prop1;

        private String prop2;

        public String getProp2() {
            return prop2;
        }

        public String getProp1() {
            return prop1;
        }

        public void setProp2(String prop2) {
            this.prop2 = prop2;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }
    }

    /**
     * ファイルの内容を文字列に変換する。
     *
     * @param file ファイル
     * @return 文字列
     */
    public static String fileToString(File file) {

        StringBuilder builder = new StringBuilder();
        InputStreamReader inputStream = null;
        try {
            inputStream = new InputStreamReader(new FileInputStream(file));
            char[] charBuffer = new char[1024];
            int length = 0;
            while ((length = inputStream.read(charBuffer)) >= 0) {
                builder.append(charBuffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(inputStream);
        }

        return builder.toString();
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File e : file.listFiles()) {
                delete(e);
            }
        }
        FileUtil.deleteFile(file);
    }
}
