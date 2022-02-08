package com.moandjiezana.toml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class IdentifierConverter {

    static final IdentifierConverter IDENTIFIER_CONVERTER = new IdentifierConverter();

    Identifier convert(String s, AtomicInteger index, Context context) {
        String rightComment = null;
        boolean terminated = false;
        boolean isKey = s.charAt(index.get()) != '[';
        boolean isTableArray = !isKey && s.length() > index.get() + 1 && s.charAt(index.get() + 1) == '[';
        int keyStartIndex = index.get();
        String key = null;
        List<String> tableOrTableArrayKey = null;
        if (isKey) {
            char keyStartChar = s.charAt(keyStartIndex);
            ReadKeyResult keyReadResult = GeneralStringKeyRead.readStrKeyAndMvIndexToEqualSplit(s, index);
            String readStrKey = keyReadResult.key;
            if (!keyReadResult.isValid) {
                context.errors.invalidKey(s.substring(keyStartIndex), context.line.get());
                return Identifier.INVALID;
            }else if (keyStartChar == '\"'|| keyStartChar== '\'') {
                key = readStrKey;
            } else{
                if (StringValueReaderWriter.QUOTE_KEY_PATTEN.matcher(readStrKey).matches()) {
                    key = readStrKey;
                } else {
                    context.errors.invalidKey(readStrKey, context.line.get());
                    return Identifier.INVALID;
                }
            }
            if (key != null) {
                terminated = searchKeyEndIndex(s, index);
            }
        }else if(!isTableArray){
            index.incrementAndGet();
            tableOrTableArrayKey = searchWrapperKey(s, index, "]");
            if (tableOrTableArrayKey != null) {
                terminated = true;
            }
        }else if(isTableArray){
            index.incrementAndGet();
            index.incrementAndGet();
            tableOrTableArrayKey = searchWrapperKey(s, index, "]]");
            if (tableOrTableArrayKey != null) {
                terminated = true;
            }
        }

        if (!terminated) {
            if (isTableArray) {
                context.errors.invalidTableArray(s.substring(keyStartIndex, index.get()), context.line.get());
            } else {
                context.errors.invalidTable(s.substring(keyStartIndex,index.get()), context.line.get());
            }

            return Identifier.INVALID;
        }
        Identifier from;
        if (isKey) {
            from = new Identifier(key, Identifier.Type.KEY);
        }else if(isTableArray){
            rightComment = subRightComment(s, index);
            if (rightComment == null) {
                context.errors.invalidTableArray(s.substring(keyStartIndex,index.get()),context.line.get());
                return Identifier.INVALID;
            }
            from = new Identifier(tableOrTableArrayKey, Identifier.Type.TABLE_ARRAY);
        }else{
            rightComment = subRightComment(s, index);
            if (rightComment == null) {
                context.errors.invalidTable(s.substring(keyStartIndex,index.get()),context.line.get());
                return Identifier.INVALID;
            }
            from = new Identifier(tableOrTableArrayKey, Identifier.Type.TABLE);
        }
        if (rightComment != null) {
            from.setRightComment(rightComment);
        }
        return from;
    }

    /**
     * 截取 table或者 tableArray的右边的注释 不包含换行符
     * @param s
     * @param index
     * @return null 非法的注释  比如 table 这样定义 [asd]123 注释 123是非法的不是以 空格与 #开头
     *         ""  没有注释
     */
    private String subRightComment(String s, AtomicInteger index) {
        int startIndex = index.get();
        boolean hasCommentStart = false;
        boolean isEndWithLineBreak = false;
        if (index.get() < s.length()) {
            while (index.get() < s.length()) {
                char c = s.charAt(index.get());
                if (c == '\n') {
                    isEndWithLineBreak = true;
                    break;
                }
                index.incrementAndGet();
                if (!Character.isWhitespace(c) &&!hasCommentStart){
                    if (c == '#') {
                        hasCommentStart = true;
                        continue;
                    } else {
                        return null;
                    }
                }
            }
            int endIndex = index.get();
            if (isEndWithLineBreak) {
                index.decrementAndGet();
            }
            return s.substring(startIndex, endIndex);
        } else {
            return "";
        }
    }

    public static List<String> searchWrapperKey(String s, AtomicInteger index, String endMark) {
        List<String> keys = new ArrayList<>();
        boolean searchNextSplitOrEndMark = false;
        outside: while (true) {
            int searchIndex = index.get();
            if (searchIndex >= s.length()) {
                keys = null;
                break;
            }
            char c = s.charAt(searchIndex);
            if (Character.isWhitespace(c)) {
                index.incrementAndGet();
                continue;
            }
            if (c == '#') {
                index.set(jumpToNextLine(s,searchIndex));
                continue;
            }
            if (searchNextSplitOrEndMark) {
                if (s.startsWith(endMark, searchIndex)) {
                    index.set(searchIndex + endMark.length());
                    break;
                }else if (c == '.') {
                    searchNextSplitOrEndMark = false;
                    index.incrementAndGet();
                    continue;
                }else{
                    keys = null;
                    break;
                }
            }

            if (c == '\"' || c=='\'') {
                String result = GeneralStringKeyRead.readQuotesString(s, index);
                if (result == null) {
                    keys = null;
                    break;
                }
                keys.add(result);
                index.incrementAndGet();
                searchNextSplitOrEndMark = true;
                continue;
            } else {
                StringBuilder keyBuilder = new StringBuilder();
                while (true) {
                    char keyChar = s.charAt(index.get());
                    if (StringValueReaderWriter.isQuoteKey(keyChar)) {
                        keyBuilder.append(keyChar);
                    } else {
                        break;
                    }
                    index.incrementAndGet();
                }
                if (keyBuilder.length() == 0) {
                    keys = null;
                    break outside;
                }
                keys.add(keyBuilder.toString());
                searchNextSplitOrEndMark = true;
                continue;
            }
        }
        return (keys == null || keys.isEmpty()) ? null : keys;
    }

    static int jumpToNextLine(String s, int searchIndex) {
        while (searchIndex < s.length()) {
            if (s.charAt(searchIndex) == '\n') {
                searchIndex++;
                return searchIndex;
            }
            searchIndex++;
        }
        return s.length();
    }

    /**
     * 查找index后面的第一个的 = 符号 某个key的结束索引
     *
     * @param s
     * @param index
     * @return
     */
    private boolean searchKeyEndIndex(String s, AtomicInteger index) {
        boolean terminated = false;
        int kVSplitSearchStartIndex = index.get();
        while (kVSplitSearchStartIndex < s.length()) {
            char c = s.charAt(kVSplitSearchStartIndex);
            if (c == '=') {
                terminated = true;
                break;
            }
            if (!Character.isWhitespace(c)) {
                break;
            }
            kVSplitSearchStartIndex++;
        }
        index.set(kVSplitSearchStartIndex);
        return terminated;
    }

    private IdentifierConverter() {}
}
