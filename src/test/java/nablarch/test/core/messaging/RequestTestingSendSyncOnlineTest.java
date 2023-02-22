package nablarch.test.core.messaging;

import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.SyncMessage;
import nablarch.test.core.http.BasicAdvice;
import nablarch.test.core.http.BasicHttpRequestTestTemplate;
import nablarch.test.core.http.TestCaseInfo;
import nablarch.test.core.log.LogVerifier;
import nablarch.test.core.messaging.sample.W11AD01Action;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * 画面オンライン処理でメッセージ同期送信を行うテスト。
 *
 * @author Masato Inoue
 */
public class RequestTestingSendSyncOnlineTest extends BasicHttpRequestTestTemplate {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/messaging/web/web-component-configuration-request-testing.xml");

    /** {@inheritDoc} */
    @Override
    protected String getBaseUri() {
        return "/action/W11AD01Action/";
    }

    /**
     * 画面オンラインでメッセージ同期送信を行うテスト。
     */
    @Test
    public void testOnline() {
        W11AD01Action.timeoutTest = true;

        execute("testSendSync", new BasicAdvice() {
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {

                // 1回目のメッセージ同期送信の応答メッセージのアサート
                SyncMessage responseMessage = context.getRequestScopedVar("responseRM11AD0101_1");
                // ヘッダ
                assertThat(responseMessage.getHeaderRecord()
                                          .get("requestId")
                                          .toString(), is("RM11AD0101"));
                assertThat(responseMessage.getHeaderRecord()
                                          .get("resendFlag")
                                          .toString(), is("9"));
                assertThat(responseMessage.getHeaderRecord()
                                          .get("testCount")
                                          .toString(), is("123"));
                assertNull(responseMessage.getHeaderRecord()
                                          .get("reserved"));
                // 本文
                assertThat(responseMessage.getDataRecord()
                                          .get("failureCode")
                                          .toString(), is("test1"));
                assertThat(responseMessage.getDataRecord()
                                          .get("userInfoId")
                                          .toString(), is("user1"));

                // 2回目のメッセージ同期送信の応答メッセージのアサート
                SyncMessage responseMessage2 = context.getRequestScopedVar("responseRM11AD0101_2");
                // ヘッダ
                assertThat(responseMessage2.getHeaderRecord()
                                           .get("requestId")
                                           .toString(), is("test"));
                assertThat(responseMessage2.getHeaderRecord()
                                           .get("resendFlag")
                                           .toString(), is("9"));
                assertThat(responseMessage2.getHeaderRecord()
                                           .get("testCount")
                                           .toString(), is("123"));
                assertNull(responseMessage.getHeaderRecord()
                                          .get("reserved"));
                // 本文
                assertThat(responseMessage2.getDataRecord()
                                           .get("failureCode")
                                           .toString(), is("test1_2"));
                assertThat(responseMessage2.getDataRecord()
                                           .get("userInfoId")
                                           .toString(), is("user1_1"));

                // 3回目のメッセージ同期送信の応答メッセージのアサート
                SyncMessage responseMessage3 = context.getRequestScopedVar("responseRM11AD0102");
                // ヘッダ
                assertThat(responseMessage3.getHeaderRecord()
                                           .get("requestId")
                                           .toString(), is("RM11AD0102"));
                assertThat(responseMessage3.getHeaderRecord()
                                           .get("resendFlag")
                                           .toString(), is(""));
                assertThat(responseMessage3.getHeaderRecord()
                                           .get("testCount")
                                           .toString(), is("123"));
                assertNull(responseMessage.getHeaderRecord()
                                          .get("reserved"));
                // 本文
                assertThat(responseMessage3.getDataRecord()
                                           .get("failureCode")
                                           .toString(), is("test2"));
                assertThat(responseMessage3.getDataRecord()
                                           .get("userInfoId")
                                           .toString(), is("user2"));
                assertThat(responseMessage3.getDataRecord()
                                           .get("test")
                                           .toString(), is("hoge"));


                // 4回目のメッセージ同期送信の応答メッセージのアサート
                SyncMessage responseMessage4 = context.getRequestScopedVar("responseRM11AD0101_1");
                // ヘッダ
                assertThat(responseMessage4.getHeaderRecord()
                                           .get("requestId")
                                           .toString(), is("RM11AD0101"));
                assertThat(responseMessage4.getHeaderRecord()
                                           .get("resendFlag")
                                           .toString(), is("9"));
                assertThat(responseMessage4.getHeaderRecord()
                                           .get("testCount")
                                           .toString(), is("123"));
                assertNull(responseMessage4.getHeaderRecord()
                                           .get("reserved"));
                // 本文
                assertThat(responseMessage4.getDataRecord()
                                           .get("failureCode")
                                           .toString(), is("test1"));
                assertThat(responseMessage4.getDataRecord()
                                           .get("userInfoId")
                                           .toString(), is("user1"));


            }
        });

        W11AD01Action.timeoutTest = false;

    }

    /**
     * expectedMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testExpectedMessageNotExist1() {
        try {
            execute("testExpectedMessageNotExist1");
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. " +
                            "case number=[1], message id=[notExist], data type=[EXPECTED_REQUEST_HEADER_MESSAGES], " +
                            "path=[src/test/java/nablarch/test/core/messaging], " +
                            "resource name=[RequestTestingSendSyncOnlineTest/testExpectedMessageNotExist1].",
                    e.getMessage());
        }
    }

    /**
     * expectedMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testExpectedMessageNotExist2() {
        try {
            execute("testExpectedMessageNotExist2");
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. " +
                            "case number=[1], message id=[notExist], data type=[EXPECTED_REQUEST_BODY_MESSAGES], " +
                            "path=[src/test/java/nablarch/test/core/messaging], " +
                            "resource name=[RequestTestingSendSyncOnlineTest/testExpectedMessageNotExist2].",
                    e.getMessage());
        }
    }


    /**
     * responseMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @SuppressWarnings("serial")
    @Test
    public void testResponseMessageNotExist1() {
        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("logLevel", "FATAL");
                put("message1", "message was not found. message must be set. " +
                        "case number=[1], message id=[notExist], data type=[RESPONSE_HEADER_MESSAGES], " +
                        "path=[src/test/java/nablarch/test/core/messaging], resource name=[RequestTestingSendSyncOnlineTest/testResponseMessageNotExist1].");
            }});
        }});
        try {
            execute("testResponseMessageNotExist1");
            fail();
        } catch (Error e) {
            LogVerifier.verify("responseMessageの識別子に対応するテーブルが存在しない場合");
        }
    }

    /**
     * responseMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @SuppressWarnings("serial")
    @Test
    public void testResponseMessageNotExist2() {
        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("logLevel", "FATAL");
                put("message1", "message was not found. message must be set. " +
                        "case number=[1], message id=[notExist], data type=[RESPONSE_BODY_MESSAGES], " +
                        "path=[src/test/java/nablarch/test/core/messaging], resource name=[RequestTestingSendSyncOnlineTest/testResponseMessageNotExist2].");
            }});
        }});
        try {
            execute("testResponseMessageNotExist2");
            fail();
        } catch (Error e) {
            LogVerifier.verify("responseMessageの識別子に対応するテーブルが存在しない場合");
        }
    }

}
