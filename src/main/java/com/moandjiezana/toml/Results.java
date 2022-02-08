package com.moandjiezana.toml;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Results {

    final Container.Table rootTable;

    static class Errors {
    
    private final StringBuilder sb = new StringBuilder();
    
    void duplicateTable(String table, int line) {
      sb.append("Duplicate table definition on line ")
        .append(line)
        .append(" ")
        .append(table);
    }

    public void tableDuplicatesKey(String table, AtomicInteger line) {
      sb.append("Key already exists for table defined on line ")
        .append(line.get())
        .append(": [")
        .append(table)
        .append("]");
    }

    public void keyDuplicatesTable(String key, AtomicInteger line) {
      sb.append("Table already exists for key defined on line ")
        .append(line.get())
        .append(": ")
        .append(key);
    }

    
    void invalidTable(String table, int line) {
      sb.append("Invalid table definition on line ")
        .append(line)
        .append(": ")
        .append(table);
    }
    
    void duplicateKey(String key, int line) {
      sb.append("Duplicate key");
      if (line > -1) {
        sb.append(" on line ")
          .append(line);
      }
      sb.append(": ")
        .append(key);
    }
    
    void invalidTextAfterIdentifier(Identifier identifier, char text, int line) {
      sb.append("Invalid text after key ")
        .append(identifier.getName())
        .append(" on line ")
        .append(line)
        .append(". Make sure to terminate the value or add a comment (#).");
    }
    
    void invalidKey(String key, int line) {
      sb.append("Invalid key on line ")
        .append(line)
        .append(": ")
        .append(key);
    }
    
    void invalidTableArray(String tableArray, int line) {
      sb.append("Invalid table array definition on line ")
        .append(line)
        .append(": ")
        .append(tableArray);
    }
    
    void invalidValue(String key, String value, int line) {
      sb.append("Invalid value on line ")
        .append(line)
        .append(": ")
        .append(key)
        .append(" = ")
        .append(value);
    }
    
    void unterminatedKey(String key, int line) {
      sb.append("Key is not followed by an equals sign on line ")
        .append(line)
        .append(": ")
        .append(key);
    }
    
    void unterminated(String key, String value, int line) {
      sb.append("Unterminated value on line ")
        .append(line)
        .append(": ")
        .append(key)
        .append(" = ")
        .append(value.trim());
    }

    public void heterogenous(String key, int line) {
      sb.append(key)
        .append(" becomes a heterogeneous array on line ")
        .append(line);
    }
    
    boolean hasErrors() {
      return sb.length() > 0;
    }
    
    @Override
    public String toString() {
      return sb.toString();
    }

    public void add(Errors other) {
      sb.append(other.sb);
    }
  }
  
  final Errors errors = new Errors();
  private final Deque<Container> stack = new ArrayDeque<Container>();
  private String lastComment;

    public String getLastComment() {
        return lastComment;
    }

    public void setLastComment(String lastComment) {
        this.lastComment = lastComment;
    }

    Results() {
        Container.Table rootTable = new Container.Table("");
        this.rootTable = rootTable;
        stack.push(rootTable);
  }

  void addValue(Identifier identifier,String key, Object value, AtomicInteger line) {
    Container currentTable = stack.peek();
      String beforeComment = null;
      String rightComment = null;
      if (identifier != null) {
          beforeComment = identifier.getBeforeComment();
          rightComment = identifier.getRightComment();
      }
    if (value instanceof Map) {
     List<String> keysInLine = getInlineTablePath(key);
      if (keysInLine == null) {
        startTable(identifier,key, line);
      } else if (keysInLine.isEmpty()) {
          List<String> keys = new ArrayList<>();
          keys.add(key);
          startTables(new Identifier(keys, Identifier.Type.TABLE), line);
      } else {
        startTables(new Identifier(keysInLine, Identifier.Type.TABLE), line);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> valueMap = (Map<String, Object>) value;
      for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
        addValue(null,entry.getKey(), entry.getValue(), line);
      }
      stack.pop();
    } else if (currentTable.accepts(key)) {
        if (identifier!=null && identifier.isTableArray()) {
            currentTable.put(key, value, "", "");
        } else {
            currentTable.put(key, value, beforeComment, rightComment);
        }
    } else {
      if (currentTable.get(key) instanceof Container) {
        errors.keyDuplicatesTable(key, line);
      } else {
        errors.duplicateKey(key, line != null ? line.get() : -1);
      }
    }
  }

  void startTableArray(Identifier identifier, AtomicInteger line) {
      String beforeComment = null;
      String rightComment = null;
      if (identifier != null) {
          beforeComment = identifier.getBeforeComment();
          rightComment = identifier.getRightComment();
      }
    String tableName = identifier.getBareName();
    while (stack.size() > 1) {
      stack.pop();
    }

    Keys.Key[] tableParts = identifier.getKeys();
    for (int i = 0; i < tableParts.length; i++) {
      String tablePart = tableParts[i].name;
      Container currentContainer = stack.peek();

      if (currentContainer.get(tablePart) instanceof Container.TableArray) {
        Container.TableArray currentTableArray = (Container.TableArray) currentContainer.get(tablePart);
        stack.push(currentTableArray);

        if (i == tableParts.length - 1) {
          currentTableArray.put(tablePart, new Container.Table(),beforeComment, rightComment);
        }

        stack.push(currentTableArray.getCurrent());
        currentContainer = stack.peek();
      } else if (currentContainer.get(tablePart) instanceof Container.Table && i < tableParts.length - 1) {
        Container nextTable = (Container) currentContainer.get(tablePart);
        stack.push(nextTable);
      } else if (currentContainer.accepts(tablePart)) {
        Container newContainer = i == tableParts.length - 1 ? new Container.TableArray(beforeComment, rightComment) : new Container.Table();
        addValue(identifier, tablePart, newContainer, line);
        stack.push(newContainer);

        if (newContainer instanceof Container.TableArray) {
          stack.push(((Container.TableArray) newContainer).getCurrent());
        }
      } else {
        errors.duplicateTable(tableName, line.get());
        break;
      }
    }
  }

  void startTables(Identifier id, AtomicInteger line) {
    String tableName = id.getBareName();
    
    while (stack.size() > 1) {
      stack.pop();
    }

    Keys.Key[] tableParts = id.getKeys();
    for (int i = 0; i < tableParts.length; i++) {
      String tablePart = tableParts[i].name;
      Container currentContainer = stack.peek();
      if (currentContainer.get(tablePart) instanceof Container) {
        Container nextTable = (Container) currentContainer.get(tablePart);
        if (i == tableParts.length - 1 && !nextTable.isImplicit()) {
          errors.duplicateTable(tableName, line.get());
          return;
        }
        stack.push(nextTable);
        if (stack.peek() instanceof Container.TableArray) {
          stack.push(((Container.TableArray) stack.peek()).getCurrent());
        }
      } else if (currentContainer.accepts(tablePart)) {
          Identifier startTableId = id;
          if (i != tableParts.length - 1) {
              startTableId = null;
          }
        startTable(startTableId,tablePart, i < tableParts.length - 1, line);
      } else {
        errors.tableDuplicatesKey(tablePart, line);
        break;
      }
    }
  }

  /**
   * Warning: After this method has been called, this instance is no longer usable.
   */
  Map<String, Object> consume() {
      Container values = rootTable;
      stack.clear();

      return ((Container.Table) values).getValueMap();
  }

    public Container.Table getLastContainer() {
        Container values = stack.getLast();
        return (Container.Table) values;
    }

    private Container startTable(Identifier identifier,String tableName, AtomicInteger line) {
    Container newTable = new Container.Table(tableName);
    addValue(identifier,tableName, newTable, line);
    stack.push(newTable);

    return newTable;
  }

  private Container startTable(Identifier identifier,String tableName, boolean implicit, AtomicInteger line) {
    Container newTable = new Container.Table(tableName, implicit);
    addValue(identifier,tableName, newTable, line);
    stack.push(newTable);

    return newTable;
  }
  
  private List<String> getInlineTablePath(String key) {
    Iterator<Container> descendingIterator = stack.descendingIterator();
    List<String> keys = new ArrayList<>();
    while (descendingIterator.hasNext()) {
      Container next = descendingIterator.next();
      if (next instanceof Container.TableArray) {
        return null;
      }
      
      Container.Table table = (Container.Table) next;
        if (table == this.rootTable) {
            continue;
        }
      if (table.name == null) {
        break;
      }

      keys.add(table.name);

    }

      keys.add(key);

      return keys;
  }
}