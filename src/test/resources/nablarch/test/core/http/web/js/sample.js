// アプリケーションで作成するJavaScript関数
function popUpConfirmation(event, element) {
    if (window.confirm("登録します。よろしいですか？")) {
        // OK
        // フレームワークが出力するJavaScript関数を明示的に呼び出す。
        return nablarch_submit(event, element);
    } else {
        // キャンセル
        return false;
    }
}
