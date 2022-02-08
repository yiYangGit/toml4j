package com.moandjiezana.toml;

import java.util.ArrayList;
import java.util.List;

class Identifier {

    static final Identifier INVALID = new Identifier("", null);

  private  String name;
  private final Type type;
  private  List<String> nameKeys;
    //某一个配置的上边的注释
  private String beforeComment;
  //某一个配置的右边的注释
  private String rightComment;

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


    /**
     * 普通的key
     * @param name
     */
   Identifier(String name,Type type) {
    this.name = name;
       this.type = type;
  }



    Identifier(List<String> keys, Type type) {
        if (type != Type.TABLE && type != Type.TABLE_ARRAY) {
            throw new IllegalArgumentException();
        }
        this.nameKeys = keys;
        this.type = type;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.nameKeys.size(); i++) {
            builder.append(nameKeys.get(i));
            if (i != this.nameKeys.size() - 1) {
                builder.append(".");
            }
        }
        if (isTable()) {
            builder.insert(0, "[");
            builder.append( "]");
        } else if (isTableArray()) {
            builder.insert(0, "[[");
            builder.append("]]");
        }
        this.name = builder.toString();
    }
  
  String getName() {
      return this.name;
  }
  
  String getBareName() {
    if (isKey()) {
      return name;
    }
    
    if (isTable()) {
      return name.substring(1, name.length() - 1);
    }
    
    return name.substring(2, name.length() - 2);
  }
    /**
     * 仅仅只能是table 或者 TableArray 才可以调用该方法
     * @return
     */
  Keys.Key[] getKeys(){
      if (isTableArray() || isTable()) {
          List<Keys.Key> keys = new ArrayList<>();
          for (String nameKey : this.nameKeys) {
              keys.add(new Keys.Key(nameKey, -1, null));
          }
          return keys.toArray(new Keys.Key[0]);
      } else {
          return null;
      }
  }
  
  boolean isKey() {
    return type == Type.KEY;
  }
  
  boolean isTable() {
    return type == Type.TABLE;
  }
  
  boolean isTableArray() {
    return type == Type.TABLE_ARRAY;
  }
  
  public static enum Type {
    KEY, TABLE, TABLE_ARRAY;
  }
}
