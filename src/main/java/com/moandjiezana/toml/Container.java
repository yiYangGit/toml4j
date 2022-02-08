package com.moandjiezana.toml;

import java.util.*;

abstract class Container {

  abstract boolean accepts(String key);
  abstract void put(String key, Object value,String beforeComment,String rightComment);
  abstract Object get(String key);
  abstract boolean isImplicit();

  static class Table extends Container {

      public Map<String, Object> getTableValues() {
          return values;
      }

      public String getName() {
          return name;
      }

      public Map<String, ContainerComment> getCommentMap() {
          return commentMap;
      }

      //Object 可能为  Table TableArray String Date double boolean Map<String,Object> 以及 toml支持的数组结构
      private  Map<String, Object> values = new LinkedHashMap<String, Object>();
    private final Map<String, ContainerComment> commentMap = new LinkedHashMap<String, ContainerComment>();
    final String name;
    final boolean implicit;

    Table() {
      this(null, false);
    }
    
    public Table(String name) {
      this(name, false);
    }

    public Table(String tableName, boolean implicit) {
      this.name = tableName;
      this.implicit = implicit;
    }

    @Override
    boolean accepts(String key) {
      return !values.containsKey(key) || values.get(key) instanceof Container.TableArray;
    }

    @Override
    void put(String key, Object value,String beforeComment,String rightComment) {
        if (value instanceof Collection &&!PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER.canWrite(value)) {
            Collection<Map<String, Object>> maps = (Collection<Map<String, Object>>) value;
            TableArray tableArray = new TableArray();
            values.put(key, tableArray);
            boolean hasSetFirstComment = false;
            for (Map<String, Object> map : maps) {
                String tableBeforeComment =null;
                String tableRightComment = null;
                if (!hasSetFirstComment) {
                    tableBeforeComment = beforeComment;
                    tableRightComment = rightComment;
                    hasSetFirstComment = true;
                }
                Table table = new Table();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    table.put(entry.getKey(), entry.getValue(), null, null);
                }
                tableArray.addTable(table, tableBeforeComment, tableRightComment);
            }
        } else {
            values.put(key, value);
            commentMap.put(key, new ContainerComment(beforeComment, rightComment));
        }
    }

      /**
       * 数据结构转换为map结构
       * @return
       */
      public LinkedHashMap<String, Object> getValueMap() {
          LinkedHashMap<String, Object> map = new LinkedHashMap<>();
          for (Map.Entry<String, Object> entry : this.values.entrySet()) {
              String key = entry.getKey();
              Object value = entry.getValue();
              if (value instanceof Table) {
                  Table table = (Table) value;
                  map.put(key, table.getValueMap());
              } else if (value instanceof TableArray) {
                  TableArray tableArray = (TableArray) value;
                  map.put(key,tableArray.getValueList());
              } else {
                  map.put(key, value);
              }
          }
          return map;
      }
      public Table setValueMap(LinkedHashMap<String, Object> valueMap){
          LinkedHashMap<String, Object> beforeValueCopy = new LinkedHashMap<>(this.values);
          this.values.clear();
          for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
              String key = entry.getKey();
              Object value = entry.getValue();
              ValueWriter writerFor = ValueWriters.WRITERS.findWriterFor(value);
              if (writerFor == ObjectValueWriter.OBJECT_VALUE_WRITER) {
                  throw new IllegalArgumentException();
              }else if (writerFor == DateValueReaderWriter.DATE_PARSER_JDK_6 || writerFor == DateValueReaderWriter.DATE_VALUE_READER_WRITER) {
                  this.values.put(key, value);
              }else if (writerFor ==PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER) {
                  this.values.put(key, value);
              }else if (writerFor ==StringValueReaderWriter.STRING_VALUE_READER_WRITER) {
                  this.values.put(key, value);
              }else if (writerFor ==BooleanValueReaderWriter.BOOLEAN_VALUE_READER_WRITER) {
                  this.values.put(key, value);
              }else if (writerFor ==NumberValueReaderWriter.NUMBER_VALUE_READER_WRITER) {
                  this.values.put(key, value);
              }else if (writerFor ==MapValueWriter.MAP_VALUE_WRITER) {
                  Object o = beforeValueCopy.get(key);
                  Map<String, Object> mapValue = (Map<String, Object>) value;
                  LinkedHashMap<String, Object> replaceValue = new LinkedHashMap<>(mapValue);
                  if (!(o instanceof Table)) {
                      Table table = new Table();
                      table.setValueMap(replaceValue);
                      this.values.put(key, table);
                  } else {
                      Table oldTable = (Table) o;
                      oldTable.setValueMap(replaceValue);
                      this.values.put(key, oldTable);
                  }
              }else if (writerFor == TableArrayValueWriter.TABLE_ARRAY_VALUE_WRITER) {
                  Object o = beforeValueCopy.get(key);
                  Collection<Map<String, Object>> mapCollection = (Collection<Map<String, Object>>) value;
                  if (o instanceof TableArray && ((TableArray) o).getTableValue().size() == mapCollection.size()) {
                      TableArray tableArray = (TableArray) o;
                      List<Table> tableValues = tableArray.getTableValue();
                      int tableIndex = 0;
                      for (Map<String, Object> vMap : mapCollection) {
                          tableValues.get(tableIndex).setValueMap(new LinkedHashMap<>(vMap));
                          tableIndex++;
                      }
                      this.values.put(key, tableArray);
                  } else {
                      TableArray newTableArray = new TableArray();
                      for (Map<String, Object> v : mapCollection) {
                          Table table = new Table();
                          table.setValueMap(new LinkedHashMap<String, Object>(v));
                          newTableArray.addTable(table,null,null);
                      }
                      this.values.put(key, newTableArray);
                  }
              } else {
                  throw new IllegalArgumentException();
              }
          }
          return this;
      };

    @Override
    Object get(String key) {
      return values.get(key);
    }
    
    boolean isImplicit() {
      return implicit;
    }

    /**
     * This modifies the Table's internal data structure, such that it is no longer usable.
     *
     * Therefore, this method must only be called when all data has been gathered.

     * @return A Map-and-List-based of the TOML data
     */
    Map<String, Object> consume() {
      for (Map.Entry<String, Object> entry : values.entrySet()) {
        if (entry.getValue() instanceof Container.Table) {
          entry.setValue(((Container.Table) entry.getValue()).consume());
        } else if (entry.getValue() instanceof Container.TableArray) {
          entry.setValue(((Container.TableArray) entry.getValue()).getValues());
        }
      }

      return values;
    }

    @Override
    public String toString() {
      return values.toString();
    }

      public boolean isEmpty() {
          return values.isEmpty();
      }

      public void setValues(Map<String, Object> values) {
          this.values = values;
      }
  }



    static class TableArray extends Container {
    private final List<Container.Table> values = new ArrayList<Container.Table>();
    private final List<ContainerComment> comments = new ArrayList<ContainerComment>();

      public List<ContainerComment> getComments() {
          return comments;
      }

      TableArray() {

      }
      TableArray(String beforeComment,String rightComment) {
      values.add(new Container.Table());
          comments.add(new ContainerComment(beforeComment, rightComment));
    }

        public List<Map<String,Object>> getValueList(){
            ArrayList<Map<String, Object>> list = new ArrayList<>();
            for (Table value : this.values) {
                list.add(value.getValueMap());
            }
            return list;
        };

    @Override
    boolean accepts(String key) {
      return getCurrent().accepts(key);
    }

    @Override
    void put(String key, Object value,String beforeComment,String rightComment) {
      values.add((Container.Table) value);
      comments.add(new ContainerComment(beforeComment, rightComment));
    }

    void addTable(Table table,String beforeComment,String rightComment) {
        values.add(table);
        comments.add(new ContainerComment(beforeComment, rightComment));
    }

    @Override
    Object get(String key) {
      throw new UnsupportedOperationException();
    }
    
    boolean isImplicit() {
      return false;
    }

      public List<Container.Table> getTableValue() {
          return values;
      }

    public List<Map<String, Object>> getValues() {
      ArrayList<Map<String, Object>> unwrappedValues = new ArrayList<Map<String,Object>>();
      for (Container.Table table : values) {
        unwrappedValues.add(table.consume());
      }
      return unwrappedValues;
    }

    Container.Table getCurrent() {
      return values.get(values.size() - 1);
    }

    @Override
    public String toString() {
      return values.toString();
    }
  }

  private Container() {}
}


class ContainerComment {
    static ContainerComment EMPTY_CONTAINER_COMMENT = new ContainerComment(null,null);
    private String beforeComment;
    private String rightComment;

    public ContainerComment(String beforeComment, String rightComment) {
        if (beforeComment == null) {
            beforeComment = "";
        }
        if (rightComment == null) {
            rightComment = "";
        }
        this.beforeComment = beforeComment;
        this.rightComment = rightComment;
    }

    public String getBeforeComment() {
        return beforeComment;
    }

    public void setBeforeComment(String beforeComment) {
        this.beforeComment = beforeComment;
    }

    public String getRightComment() {
        return rightComment;
    }

    public void setRightComment(String rightComment) {
        this.rightComment = rightComment;
    }
}