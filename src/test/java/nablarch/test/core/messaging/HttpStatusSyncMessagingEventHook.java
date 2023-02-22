package nablarch.test.core.messaging;

import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.SyncMessagingEventHook;
import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;

import java.util.Map;
import java.util.TreeMap;

/**
 * メッセージ送信の処理前後に処理を行うためのインターフェイス。<br>
 * 
 * @author Masaya Seko
 */
public class HttpStatusSyncMessagingEventHook implements SyncMessagingEventHook {

    /**
     * メッセージ送信前に呼ばれる処理。
     * 
     * @param settings メッセージ送信設定
     * @param requestMessage 送信対象メッセージ
     */
    @Override
    public void beforeSend(MessageSenderSettings settings, SyncMessage requestMessage) {
        //何もしない
        return;
    }

    /**
     * メッセージ送信後、レスポンスを受け取った後に呼ばれる処理。<br>
     * <p>
     * ステータスコードをチェックして、正常終了であるか、業務エラーであるかを判定します。
     * </p>
     * @param settings メッセージ送信設定
     * @param requestMessage リクエストメッセージ
     * @param responseMessage レスポンスメッセージ
     */
    @Override
    public void afterSend(MessageSenderSettings settings, SyncMessage requestMessage,
            SyncMessage responseMessage) {
        String statusCode = (String) responseMessage.getHeaderRecord().get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE);
        if (!"200".equals(statusCode)) {
            // ステータスコード200以外の場合はシステムエラーとして扱う。
            throw new HttpMessagingException();
        }
    }

    /**
     * メッセージ送信中のエラー発生時に呼ばれる処理。<br>
     * <p>
     * ステータスコードをチェックして、業務エラーであるか、システム例外であるかを判定します。
     * </p>
     * 
     * @param e 発生した例外
     * @param hasNext 次に呼び出される{@link SyncMessagingEventHook}が存在する場合にtrue
     * @param settings メッセージ送信設定
     * @param requestMessage リクエストメッセージ
     * @param responseMessage レスポンスメッセージとして使用するオブジェクト。本オブジェクトは最終的に MessageSender#sendSync(SyncMessage)の戻り値として返却される。
     * @return trueの場合は処理継続。次の{@link SyncMessagingEventHook#onError(RuntimeException, MessageSenderSettings, SyncMessage)}を呼ぶ。<br />
     * 次がない場合は、MessageSender#sendSync(SyncMessage)の戻り値として、引数responseMessageの値を返す。<br />
     * falseの場合は、本メソッド終了後に引数eをthrowする
     */
    @Override
    public boolean onError(RuntimeException e,
            boolean hasNext, MessageSenderSettings settings, SyncMessage requestMessage, SyncMessage responseMessage) {
        if(e instanceof HttpMessagingInvalidDataFormatException){
            HttpMessagingInvalidDataFormatException dataFormatException = (HttpMessagingInvalidDataFormatException)e;
            Integer statusCode = dataFormatException.getStatusCode();
            Map<String, Object> headerRecord = new TreeMap<String, Object>();
            headerRecord.put(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE, statusCode.toString());
            responseMessage.setHeaderRecord(headerRecord);
        }
        return true;
    }
}
