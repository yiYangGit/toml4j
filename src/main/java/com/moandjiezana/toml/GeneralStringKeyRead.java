package com.moandjiezana.toml;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yangyi on 2021/2/28.

 *
 */
public class GeneralStringKeyRead {

    /**
     * 读取 "123" 或者 '123' 中的 123
     * 或者例如从 "1.1.1.1 " 的0开始读的话 读到第一个非空格字符的前面
     *或者例如 从 "1.1.1.1=" 的0开始读的话 读到第一个=符号的前面
     *
     * 最后将索引设置到下一个 = 的索引 =之前有其他的非空格字符
     * 则将设置索引索引到 该非空格字符
     * @param s
     * @param index
     * @return null 未找到字符换则非法
     */
    public static ReadKeyResult readStrKeyAndMvIndexToEqualSplit(String s, AtomicInteger index) {
        int i = index.get();
        if (i < 0 || i >= s.length()) {
            throw new IllegalArgumentException();
        }
        if (s.charAt(i) == '\'') {
            String result = readSingleQuotesKeyStrMoveIndexToEq(s, index, true);
            if (result == null) {
                result = s.substring(i, index.get());
                return new ReadKeyResult(result, false);
            }
            return new ReadKeyResult(result, true);
        }
        if (s.charAt(i) == '\"') {
            String result = StringValueReaderWriter.readStr(s, index);
            if (result == null) {
                return new ReadKeyResult(s.substring(i, index.get()), false);
            }
            index.incrementAndGet();
            if (!moveIndexToEq(s, index)) {
                return new ReadKeyResult(s.substring(i, index.get()), false);
            }

            return new ReadKeyResult(result, true);
        }

        StringBuilder keyBuilder = new StringBuilder();
        if (Character.isWhitespace(s.charAt(index.get()))) {
            throw new IllegalArgumentException();
        }
        keyBuilder.append(s.charAt(index.get()));
        while (index.incrementAndGet() < s.length()) {
            char c = s.charAt(index.get());
            if (c == '=' || Character.isWhitespace(c)) {
                break;
            } else {
                keyBuilder.append(c);
            }
        }
        if (!moveIndexToEq(s, index)) {
            return new ReadKeyResult(s.substring(i, index.get()), false);
        } else {
            return new ReadKeyResult(keyBuilder.toString(), true);
        }
    }

    /**
     * 把索引移动下个= 的位置 没有 = 则返回false
     * @param s
     * @param index
     * @return
     */
    static boolean moveIndexToEq(String s, AtomicInteger index) {
        if (index.get() >= s.length()) {
            return false;
        }
        if (s.charAt(index.get()) == '=') {
            return true;
        }
        while (index.incrementAndGet() < s.length()) {
            int i = index.get();
            if (Character.isWhitespace(s.charAt(i))) {
                continue;
            }
            return s.charAt(i) == '=';
        }
        return false;
    }

    /**
     * 读取以 ' 开头的字符串 并且将索引移动到 下一个 =
     * @param s
     * @param index
     quotes
     * @return
     */
    private static String readSingleQuotesKeyStrMoveIndexToEq(String s, AtomicInteger index,boolean isMoveIndexToEq) {
        int startIndex = index.get();
        if (startIndex >= s.length() || startIndex < 0) {
            throw new IllegalArgumentException();
        }
        if (s.charAt(startIndex) != '\'') {
            throw new IllegalArgumentException();
        }
        while (index.incrementAndGet() < s.length()) {
            int currentIndex = index.get();
            char c = s.charAt(currentIndex);
            if (c == '\n') {
                return null;
            }
            if (c == '\'') {
                if (isMoveIndexToEq && !moveIndexToEq(s, index)) {
                    return null;
                }
                return s.substring(startIndex+1, currentIndex);
            }
        }
        return null;
    }

    /**
     * 读取 以 ' 或者 " 开始与结尾的字符串 自动处理 ""中的转义字符
     * @param s
     * @param index
     * @return 字符不合法则返回 null  返回合法则 index设为结束符 ' 或 " 的索引位置
     */
    public static String readQuotesString(String s, AtomicInteger index) {
        int startIndex = index.get();
        if (startIndex >= s.length() || startIndex < 0) {
            throw new IllegalArgumentException();
        }
        if (s.charAt(startIndex) == '\'') {
            return readSingleQuotesKeyStrMoveIndexToEq(s, index, false);
        }
        if (s.charAt(startIndex) == '\"') {
            return StringValueReaderWriter.readStr(s, index);
        }
        throw new IllegalStateException();
    }
}

class ReadKeyResult {
    //可能为null
    final String key;
    //是否有效
    final boolean isValid;

    public ReadKeyResult(String key, boolean isValid) {
        this.key = key;
        this.isValid = isValid;
    }
}