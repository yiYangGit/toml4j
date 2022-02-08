package com.moandjiezana.toml;


import static com.moandjiezana.toml.BooleanValueReaderWriter.BOOLEAN_VALUE_READER_WRITER;
import static com.moandjiezana.toml.DateValueReaderWriter.DATE_PARSER_JDK_6;
import static com.moandjiezana.toml.DateValueReaderWriter.DATE_VALUE_READER_WRITER;
import static com.moandjiezana.toml.NumberValueReaderWriter.NUMBER_VALUE_READER_WRITER;
import static com.moandjiezana.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER;
import static com.moandjiezana.toml.StringValueReaderWriter.STRING_VALUE_READER_WRITER;

import java.util.*;

/**
 * Created by yangyi0 on 2021/2/23.
 */
class TomlContainer {

    //简单参数类型的writer
    private static final Set<ValueWriter> PLAIN_VALUE_WRITE = new HashSet<ValueWriter>();
    static {
        PLAIN_VALUE_WRITE.add(STRING_VALUE_READER_WRITER);
        PLAIN_VALUE_WRITE.add(BOOLEAN_VALUE_READER_WRITER);
        PLAIN_VALUE_WRITE.add(DATE_PARSER_JDK_6);
        PLAIN_VALUE_WRITE.add(DATE_VALUE_READER_WRITER);
        PLAIN_VALUE_WRITE.add(NUMBER_VALUE_READER_WRITER);
        PLAIN_VALUE_WRITE.add(PRIMITIVE_ARRAY_VALUE_WRITER);
    }
    private Container.Table container;

    public Container.Table getContainer() {
        return container;
    }

    private String lastComment;


    public LinkedHashMap<String, Object> getValueMap() {
        return this.container.getValueMap();
    }


    /**
     * 修改配置
     * @param valueMap
     *
     */
    public void setValue(LinkedHashMap<String, Object> valueMap) {
        container.setValueMap(valueMap);
    }



    /**
     * 序列化
     * @param context
     */
    public void write(WriterContext context) {
        writeTable(container,context,null);
        if (lastComment != null) {
            context.write(lastComment);
        }
    }

    private void writeTable(Container.Table container,WriterContext context,String parentPath) {
        Map<String, ContainerComment> commentMap = container.getCommentMap();
        String parentBuildPath = parentPath;
        if (parentBuildPath != null) {
            parentBuildPath = parentPath + ".";
        } else {
            parentBuildPath = "";
        }
        for (Map.Entry<String, Object> entry : container.getTableValues().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            ValueWriter writerFor = ValueWriters.WRITERS.findWriterFor(value);
            ContainerComment containerComment = commentMap.get(key);
            if (containerComment == null) {
                containerComment = ContainerComment.EMPTY_CONTAINER_COMMENT;
            }
            if (PLAIN_VALUE_WRITE.contains(writerFor)) {
                String quoteKey  = StringValueReaderWriter.quoteKey(key);
                writePlainValue(context, quoteKey, value, writerFor, containerComment);

                continue;
            }
            String tomlPath = parentBuildPath + StringValueReaderWriter.quoteKey(key);
            if (value instanceof Container.Table) {
                writeTableTitle(context, tomlPath,  containerComment);
                writeTable((Container.Table) value, context, tomlPath);
                continue;
            }
            if (value instanceof Container.TableArray) {
                Container.TableArray tableArray = (Container.TableArray) value;
                writeTableArray(tableArray,tomlPath,context);
                continue;
            }
        }
    }

    private void writeTableArray(Container.TableArray tableArray, String parentPath,WriterContext context) {
        List<Container.Table> tableValue = tableArray.getTableValue();
        List<ContainerComment> comments = tableArray.getComments();
        for (int i = 0; i < tableValue.size(); i++) {
            ContainerComment containerComment = comments.get(i);
            context.write(containerComment.getBeforeComment());
            context.write("[[");
            context.write(parentPath);
            context.write("]]");
            context.write(containerComment.getRightComment());
            context.writeNewLine();
            writeTable(tableValue.get(i),context, parentPath);
        }
    }

    private void writeTableTitle(WriterContext context, String parentBuildPath, ContainerComment containerComment) {
        context.write(containerComment.getBeforeComment());
        context.write("[");
        context.write(parentBuildPath);
        context.write("]");
        context.write(containerComment.getRightComment());
        context.writeNewLine();
    }

    private void writePlainValue(WriterContext context, String key, Object value, ValueWriter writerFor, ContainerComment containerComment) {
        if (containerComment.getBeforeComment()!=null) {
            context.write(containerComment.getBeforeComment());
        }
        context.write(key);
        context.write(" = ");
        writerFor.write(value, context);
        if (containerComment.getBeforeComment() != null) {
            context.write(containerComment.getRightComment());
        }
        context.writeNewLine();
    }


    public TomlContainer(Container.Table container, String lastComment) {
        this.container = container;
        if (lastComment == null) {
            lastComment = "";
        }
        this.lastComment = lastComment;
    }



    public boolean isEmpty() {
        return this.container.isEmpty();
    }
}
