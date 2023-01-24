package nablarch.test.core.http;

import nablarch.fw.ExecutionContext;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link AbstractHttpRequestTestTemplate}のテストクラス。
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class AbstractHttpRequestTestTemplateTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/http/http-test-configuration.xml");

    /** アップロード先ディレクトリ */
    private static File workDir = new File("work");

    /** アップロード先ディレクトリの準備 */
    @BeforeClass
    public static void prepareUploadDir() {
        workDir.delete();
        workDir.mkdir();
    }

    /** 
     * アップロード先ディレクトリの削除 
     * HttpRequestTestSupportをデフォルトに復元する
     */
    @AfterClass
    public static void deleteUploadDir() {
        workDir.delete();
        HttpRequestTestSupport.resetHttpServer();
    }

    @Before
    public void setUp() {
        HttpRequestTestSupport.resetHttpServer();
    }

    /** テスト対象 */
    private AbstractHttpRequestTestTemplate<TestCaseInfo> target;


    /**
     * セッション変数の内容をテストケース内で任意に書き換えられることを検証。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSessionScopedVarOverwrite() {

        @SuppressWarnings("rawtypes")
        final AbstractHttpRequestTestTemplate<TestCaseInfo>
                target = new AbstractHttpRequestTestTemplate(getClass()) {
            @Override
            protected String getBaseUri() {
                return "/nablarch/test/core/http/SessionOverwriteAction/";
            }
        };

        target.execute("testSessionScopedVarOverwrite", new Advice<TestCaseInfo>() {
            public void beforeExecute(TestCaseInfo info, ExecutionContext ctx) {
                ctx.setSessionScopedVar("commonHeaderLoginUserName", "リクエスト単体テストユーザ2");
                ctx.setSessionScopedVar("commonHeaderLoginDate", "20120914");
                ctx.setSessionScopedVar("otherSessionParam", "hoge");
            }

            public void afterExecute(TestCaseInfo info, ExecutionContext ctx) {
                // セッションが無効化されているので、beforeExecuteで設定した値はクリアされている。
                assertThat(ctx.getSessionScopedVar("commonHeaderLoginUserName"), nullValue());
                assertThat(ctx.getSessionScopedVar("commonHeaderLoginDate"), nullValue());
                assertThat(ctx.getSessionScopedVar("otherSessionParam"), nullValue());

                // 以下のキーは追加されていること。
                assertThat((String) ctx.getSessionScopedVar("addKey"), is("これは追加される。"));
            }
        });
    }

}
