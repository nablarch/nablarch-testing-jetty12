package nablarch.test.core.messaging.sample;

import nablarch.core.validation.ValidateFor;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationUtil;
import nablarch.core.validation.validator.Length;
import nablarch.core.validation.validator.Required;

import java.util.Map;

/**
 * メッセージ同期送信テスト用オンラインAction。
 * @author Masato Inoue
 */
public class W11AD01Form {

    private String title;
    private String publisher;
    private String authors;

    public W11AD01Form(Map<String, Object> params) {
        title = (String) params.get("title");
        publisher = (String) params.get("publisher");
        authors = (String) params.get("authors");
    }

    public String getTitle() {
        return title;
    }

    @Required
    @Length(max = 20)
    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    @Required
    @Length(max = 20)
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getAuthors() {
        return authors;
    }

    @Required
    @Length(max = 20)
    public void setAuthors(String authors) {
        this.authors = authors;
    }

    @ValidateFor("validateForSend")
    public static void validateForSend(ValidationContext<W11AD01Form> context) {
        ValidationUtil.validate(context,
                                new String[] { "title", "publisher", "authors"});
    }
}
