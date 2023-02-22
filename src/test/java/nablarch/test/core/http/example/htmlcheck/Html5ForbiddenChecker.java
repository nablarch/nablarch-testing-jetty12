package nablarch.test.core.http.example.htmlcheck;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.test.core.http.example.htmlcheck.html5parse.SimpleNode;
import nablarch.test.core.http.example.htmlcheck.html5parse.Token;
import nablarch.test.tool.htmlcheck.HtmlForbiddenNodeConf;
import nablarch.test.tool.htmlcheck.InvalidHtmlException;

/**
 * 規約上許可されていないタグ/属性が、HTML内で使われていないかをチェックする。
 *
 * @author Tomokazu Kagawa
 */
public class Html5ForbiddenChecker {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(Html5ForbiddenChecker.class);
    
    /**
     * 規約上許可されていないタグ/属性情報
     */
    private HtmlForbiddenNodeConf conf = null;

    /**
     * コンストラクタ
     *
     * @param conf 規約上許可されていないタグ/属性情報
     */
    public Html5ForbiddenChecker(HtmlForbiddenNodeConf conf) {
        this.conf = conf;
    }

    /**
     * 規約上許可されていないタグ/属性が、HTML内で使われていないかをチェックする。
     *
     * @param node チェック対象Htmlファイルの構文木
     * @throws InvalidHtmlException  チェック結果がNGの場合
     */
    public void check(SimpleNode node) throws InvalidHtmlException {

        StringBuilder errorMessage = new StringBuilder();

        checkNode(node, errorMessage);

        if (errorMessage.length() != 0) {
            throw new InvalidHtmlException(errorMessage.toString());
        }
    }

    /**
     * 指定されたSimpleNode中に規約上許可されていないタグ/属性が使用されていないことをチェックする。<br>
     * 引数のerrorMessageには、呼び出しもとで、StringBuilderのインスタンスを作成し、渡すこと。<br>
     * チェック中に生じたメッセージが格納される。
     * 
     * @param node チェック対象の構文木
     * @param errorMessage エラーメッセージ
     */
    private void checkNode(SimpleNode node, StringBuilder errorMessage) {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {

            SimpleNode child = (SimpleNode) node.jjtGetChild(i);

            // 当該ノードが使用を許可されていないタグとして登録されているかをチェックする。
            if (child.jjtGetValue() != null && !"attrName".equals(child.toString().trim())) {

                // タグ自身が許可されていない場合
                Token tagNode = (Token) child.jjtGetValue();
                String tagName = tagNode.toString().replaceAll("[<>]", "").trim().toLowerCase();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.logTrace(tagName + "-----------------------------------------------");
                }
                if (conf.isForbiddenTag(tagName)) {
                    addForbiddenTagErrorMsg(errorMessage, tagName, tagNode);
                    continue;
                }

                // タグの属性が許可されていない場合
                for (int j = 0; j < child.jjtGetNumChildren(); j++) {
                    SimpleNode grandChild = (SimpleNode) child.jjtGetChild(j);
                    if ("attr".equals(grandChild.toString().trim())) {
                        Token attrToken = (Token) ((SimpleNode) grandChild.jjtGetChild(0)).jjtGetValue();
                        String attrName = attrToken.image.trim().toLowerCase();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.logTrace(attrName);
                        }
                        if (conf.isForbiddenAttr(tagName, attrName)) {
                            addForbiddenAttrErrorMsg(errorMessage, tagName, attrToken);
                        }
                    }
                }
            }
            checkNode(child, errorMessage);
        }
    }

    /**
     * 使用されているタグが許可されていない場合のメッセージを追加する。
     * 
     * @param errorMessage エラーメッセージ
     * @param tagName タグ名
     * @param tagNode 許可されていないタグ情報
     */
    private void addForbiddenTagErrorMsg(StringBuilder errorMessage, String tagName, Token tagNode) {
        errorMessage.append("(")
                    .append(tagName)
                    .append(") at line ")
                    .append(tagNode.beginLine)
                    .append(" column ")
                    .append(tagNode.beginColumn)
                    .append(" is forbidden.\n");
    }

    /**
     * 使用されている属性が許可されていない場合のメッセージを追加する。
     * 
     * @param errorMessage エラーメッセージ
     * @param tagName タグ名
     * @param attrNode 許可されていない属性情報
     */
    private void addForbiddenAttrErrorMsg(StringBuilder errorMessage, String tagName, Token attrNode) {
        errorMessage.append("(");
        errorMessage.append(tagName);
        errorMessage.append(", ");
        errorMessage.append(attrNode.image.trim().toLowerCase());
        errorMessage.append(") at line ");
        errorMessage.append(attrNode.beginLine);
        errorMessage.append(" column ");
        errorMessage.append(attrNode.beginColumn);
        errorMessage.append(" is forbidden.\n");
    }
}
