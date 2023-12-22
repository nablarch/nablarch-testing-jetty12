package nablarch.test.core.http.dump;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * HTTPリクエストのダンプを行うクラス。
 *
 * @author T.Kawasaki
 */
class RequestDumpAgent {

    /**
     * @param uri    URI
     * @param params パラメータ
     * @param out    出力ストリーム
     * @throws IOException 予期しない入出力例外
     */
    void print(String uri, Map<String, String[]> params, OutputStream out) throws IOException {
        HSSFWorkbook book = createDumpedBook(uri, params);
        book.write(out);
    }


    /**
     * ダンプされたブックを生成する。
     *
     * @param uri    URI
     * @param params パラメータ
     * @return ブック
     * @throws IOException 予期しない入出力例外
     */
    HSSFWorkbook createDumpedBook(String uri, Map<String, String[]> params) throws IOException {
        // テンプレートのブックを読み込み
        HSSFWorkbook book = getTemplateBook("template.xls");
        HSSFSheet sheet = book.getSheet("Sheet1");
        // セル書式スタイル初期化を行う。
        HSSFCellStyle columnCellStyle = book.createCellStyle();
        drawRuledLine(columnCellStyle);
        columnCellStyle.setFillForegroundColor(HSSFColor.LIGHT_GREEN.index);
        columnCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        HSSFCellStyle valueCellStyle = book.createCellStyle();
        drawRuledLine(valueCellStyle);

        int rowIdx = 1;
        // コメント行（日時）
        writeLine(sheet, rowIdx++, "// " + new Date());
        // コメント行（リクエストパス）
        writeLine(sheet, rowIdx++, "// URI=[" + uri + "]");
        // ID行
        writeLine(sheet, rowIdx++, "LIST_MAP=");

        // カラム行
        HSSFRow columnRow = sheet.createRow(rowIdx++);
        // 値行
        HSSFRow valueRow = sheet.createRow(rowIdx++);

        // キー順にソートする。
        TreeMap<String, String[]> sortedParams = new TreeMap<String, String[]>(params);
        int columnIdx = 0;
        for (Map.Entry<String, String[]> e : sortedParams.entrySet()) {
            // カラム
            String key = e.getKey();
            HSSFCell columnCell = columnRow.createCell(columnIdx);
            setValue(columnCell, key);
            columnCell.setCellStyle(columnCellStyle);

            // 値
            String value = escapeAndJoinWithComma(e.getValue());
            HSSFCell valueCell = valueRow.createCell(columnIdx);
            setValue(valueCell, value);
            valueCell.setCellStyle(valueCellStyle);

            columnIdx++;
        }
        return book;
    }

    /**
     * 文字列のエスケープ処理を行う。<br/>
     * <ul>
     * <li>\を\\に</li>
     * <li>,を\,に</li>
     * </ul>
     *
     * @param orig 元の文字列
     * @return エスケープ後の文字列
     */
    String escape(Object orig) {
        String s = String.valueOf(orig);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c =  s.charAt(i);
            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case ',':
                    result.append("\\,");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * テンプレートのブックを取得する。
     *
     * @param resourcePath 取得対象のパス
     * @return テンプレートのブック
     * @throws IOException 予期しない入出力例外
     */
    HSSFWorkbook getTemplateBook(String resourcePath) throws IOException {

        InputStream in = RequestDumpAgent.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("can't load template file in classpath. file=[" + resourcePath + "]");
        }
        try {
            POIFSFileSystem fs = new POIFSFileSystem(in);
            return new HSSFWorkbook(fs);
        } finally {
            closeQuietly(in);
        }
    }


    /**
     * １行出力する
     *
     * @param sheet  シート
     * @param rowNum 行番号
     * @param value  出力する値
     */
    private void writeLine(HSSFSheet sheet, int rowNum, String value) {
        HSSFRow row = sheet.createRow(rowNum);
        HSSFCell cell = row.createCell(0);
        setValue(cell, value);
    }

    /**
     * セルに値を設定する。
     *
     * @param target 設定対象となるセル
     * @param value  設定する値
     */
    private void setValue(HSSFCell target, String value) {
        target.setCellValue(new HSSFRichTextString(value));
    }

    /**
     * 罫線を引く
     *
     * @param style 設定対象のセル書式
     */
    private void drawRuledLine(HSSFCellStyle style) {
        style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style.setBorderTop(HSSFCellStyle.BORDER_THIN);
    }

    /**
     * 文字列配列を結合する。<br/>
     * 区切り文字はカンマ(,)となる。
     *
     * @param array 結合対象となる文字列配列
     * @return 結合後の文字列
     */
    private String escapeAndJoinWithComma(Object[] array) {
        return escapeAndJoin(array, ",");
    }

    /**
     * 文字列配列を結合する。
     *
     * @param array     結合対象となる文字列配列
     * @param delimiter 区切り文字
     * @return 結合後の文字列
     */
    String escapeAndJoin(Object[] array, String delimiter) {
        if (array == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object e : array) {
            if (first) {       // 初回以降は区切り文字を付与
                first = false;
            } else {
                sb.append(delimiter);
            }
            String escaped = escape(e);
            sb.append(escaped);
        }
        return sb.toString();
    }

    /**
     * 例外発生なしでリソースをクローズする。
     *
     * @param closeable リソース
     */
    void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {  // SUPPRESS CHECKSTYLE
            // NOP
        }
    }
}
