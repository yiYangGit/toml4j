package com.moandjiezana.toml;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * <p>Provides access to the keys and tables in a TOML data source.</p>
 *
 * <p>All getters can fall back to default values if they have been provided as a constructor argument.
 * Getters for simple values (String, Date, etc.) will return null if no matching key exists.
 * {@link #getList(String)}, {@link #getTable(String)} and {@link #getTables(String)} return empty values if there is no matching key.</p>
 * 
 * <p>All read methods throw an {@link IllegalStateException} if the TOML is incorrect.</p>
 *
 * <p>Example usage:</p>
 * <pre><code>
 * Toml toml = new Toml().read(getTomlFile());
 * String name = toml.getString("name");
 * Long port = toml.getLong("server.ip"); // compound key. Is equivalent to:
 * Long port2 = toml.getTable("server").getLong("ip");
 * MyConfig config = toml.to(MyConfig.class);
 * </code></pre>
 *
 */
public final class Toml {

    /**
     * toml格式支持配置的基础类型
     */
    public static Set<Class<?>> SUPPORT_BASIC_CONFIG_TYPE;

    static {
        Set<Class<?>> sets = new HashSet<>();
        sets.add(Long.class);
        sets.add(Date.class);
        sets.add(Double.class);
        sets.add(String.class);
        SUPPORT_BASIC_CONFIG_TYPE = Collections.unmodifiableSet(sets);
    }

  private static final Gson DEFAULT_GSON = new Gson();


  TomlContainer tomlContainer;

  /**
   * Creates Toml instance with no defaults.
   */
  public Toml() {
    this(new Container.Table());
  }

  /**
   * Populates the current Toml instance with values from file.
   *
   * @param file The File to be read. Expected to be encoded as UTF-8.
   * @return this instance
   * @throws IllegalStateException If file contains invalid TOML
   */
  public Toml read(File file) {
    try {
      return read(new InputStreamReader(new FileInputStream(file), "UTF8"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


    public Toml(Toml from) {
        Container.Table table = new Container.Table();
        table.setValueMap(from.toMap());
        this.tomlContainer = new TomlContainer(table, "");
    }

  /**
   * Populates the current Toml instance with values from inputStream.
   *
   * @param inputStream Closed after it has been read.
   * @return this instance
   * @throws IllegalStateException If file contains invalid TOML
   */
  public Toml read(InputStream inputStream) {
    return read(new InputStreamReader(inputStream));
  }

  /**
   * Populates the current Toml instance with values from reader.
   *
   * @param reader Closed after it has been read.
   * @return this instance
   * @throws IllegalStateException If file contains invalid TOML
   */
  public Toml read(Reader reader) {
      String tomlString = readToTomlString(reader);
      read(tomlString);
    return this;
  }

    private String readToTomlString(Reader reader) {
        BufferedReader bufferedReader = null;
        StringBuilder w = new StringBuilder();
        try {
          bufferedReader = new BufferedReader(reader);


          String line = bufferedReader.readLine();
          while (line != null) {
            w.append(line).append('\n');
            line = bufferedReader.readLine();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          try {
            bufferedReader.close();
          } catch (IOException e) {}
        }
        String tomlString = w.toString();
        return tomlString;
    }

    /**
   * Populates the current Toml instance with values from otherToml.
   *
   * @param otherToml 
   * @return this instance
   */
  public Toml read(Toml otherToml) {
    this.tomlContainer = otherToml.tomlContainer;
    
    return this;
  }

  /**
   * Populates the current Toml instance with values from tomlString.
   *
   * @param tomlString String to be read.
   * @return this instance
   * @throws IllegalStateException If tomlString is not valid TOML
   */
   Toml read(String tomlString) throws IllegalStateException {
       this.tomlContainer = this.stringToTomlContainer(tomlString);

    return this;
  }

   TomlContainer readerToTomlContainer(Reader reader) throws IllegalStateException {
       String tomlString = this.readToTomlString(reader);
       return stringToTomlContainer(tomlString);
   }


   TomlContainer stringToTomlContainer(String tomlString) {
        Results results = TomlParser.run(tomlString);
        if (results.errors.hasErrors()) {
            throw new IllegalStateException(results.errors.toString());
        }
        Container.Table lastContainer = results.getLastContainer();
        return new TomlContainer(lastContainer, results.getLastComment());
    }

    String getString(String key) {
    return (String) get(key,false);
  }

   String getString(String key, String defaultValue) {
    String val = getString(key);
    return val == null ? defaultValue : val;
  }

   Long getLong(String key) {
      Object o = get(key,false);
      if (o instanceof BigDecimal) {
          BigDecimal bigDecimal = (BigDecimal) o;
          return bigDecimal.longValue();
      } else {
          return (Long) o;
      }
  }

   Long getLong(String key, Long defaultValue) {
    Long val = getLong(key);
    return val == null ? defaultValue : val;
  }

  /**
   * @param key a TOML key
   * @param <T> type of list items
   * @return <code>null</code> if the key is not found
   */
   <T> List<T> getList(String key) {
    @SuppressWarnings("unchecked")
    List<T> list = (List<T>) get(key,true);
    
    return list;
  }

  /**
   * @param key a TOML key
   * @param defaultValue a list of default values
   * @param <T> type of list items
   * @return <code>null</code> is the key is not found
   */
   <T> List<T> getList(String key, List<T> defaultValue) {
    List<T> list = getList(key);
    
    return list != null ? list : defaultValue;
  }

   Boolean getBoolean(String key) {
    return (Boolean) get(key,false);
  }

   Boolean getBoolean(String key, Boolean defaultValue) {
    Boolean val = getBoolean(key);
    return val == null ? defaultValue : val;
  }

   Date getDate(String key) {
    return (Date) get(key,false);
  }

   Date getDate(String key, Date defaultValue) {
    Date val = getDate(key);
    return val == null ? defaultValue : val;
  }

   Double getDouble(String key) {
      Object o = get(key,false);
      if (o instanceof BigDecimal) {
          BigDecimal bigDecimal = (BigDecimal) o;
          return bigDecimal.doubleValue();
      } else {
          return (Double) o;
      }
  }

   Double getDouble(String key, Double defaultValue) {
    Double val = getDouble(key);
    return val == null ? defaultValue : val;
  }

  /**
   * @param key A table name, not including square brackets.
   * @return A new Toml instance or <code>null</code> if no value is found for key.
   */
  @SuppressWarnings("unchecked")
   Toml getTable(String key) {
      Object o = get(key, false);
      Container.Table table = (Container.Table) o;

    return table != null ? new Toml(table) : null;
  }

  /**
   * @param key Name of array of tables, not including square brackets.
   * @return A {@link List} of Toml instances or <code>null</code> if no value is found for key.
   */
  @SuppressWarnings("unchecked")
   List<Toml> getTables(String key) {
    Container.TableArray tableArray = (Container.TableArray) get(key,false);

    if (tableArray == null) {
      return null;
    }

    List<Toml> tables = new ArrayList<Toml>();
    for (Container.Table table : tableArray.getTableValue()) {
          tables.add(new Toml(table));
    }
    return tables;
  }

  /**
   * @param key a key name, can be compound (eg. a.b.c)
   * @return true if key is present
   */
   boolean contains(String key) {
    return get(key,false) != null;
  }

  /**
   * @param key a key name, can be compound (eg. a.b.c)
   * @return true if key is present and is a primitive
   */
   boolean containsPrimitive(String key) {
    Object object = get(key,true);
    
    return object != null && !(object instanceof Map) && !(object instanceof List);
  }

  /**
   * @param key a key name, can be compound (eg. a.b.c)
   * @return true if key is present and is a table
   */
   boolean containsTable(String key) {
    Object object = get(key,true);
    
    return object != null && (object instanceof Map);
  }

  /**
   * @param key a key name, can be compound (eg. a.b.c)
   * @return true if key is present and is a table array
   */
   boolean containsTableArray(String key) {
    Object object = get(key,false);
    
    return object != null && (object instanceof Container.TableArray);
  }

   boolean isEmpty() {
    return tomlContainer.isEmpty();
  }

  /**
   * <p>
   *  Populates an instance of targetClass with the values of this Toml instance.
   *  The target's field names must match keys or tables.
   *  Keys not present in targetClass will be ignored.
   * </p>
   *
   * <p>Tables are recursively converted to custom classes or to {@link Map Map&lt;String, Object&gt;}.</p>
   *
   * <p>In addition to straight-forward conversion of TOML primitives, the following are also available:</p>
   *
   * <ul>
   *  <li>Integer -&gt; int, long (or wrapper), {@link java.math.BigInteger}</li>
   *  <li>Float -&gt; float, double (or wrapper), {@link java.math.BigDecimal}</li>
   *  <li>One-letter String -&gt; char, {@link Character}</li>
   *  <li>String -&gt; {@link String}, enum, {@link java.net.URI}, {@link java.net.URL}</li>
   *  <li>Multiline and Literal Strings -&gt; {@link String}</li>
   *  <li>Array -&gt; {@link List}, {@link Set}, array. The generic type can be anything that can be converted.</li>
   *  <li>Table -&gt; Custom class, {@link Map Map&lt;String, Object&gt;}</li>
   * </ul>
   *
   * @param targetClass Class to deserialize TOML to.
   * @param <T> type of targetClass.
   * @return A new instance of targetClass.
   */
   <T> T to(Class<T> targetClass) {
    JsonElement json = DEFAULT_GSON.toJsonTree(toMap());
    
    if (targetClass == JsonElement.class) {
      return targetClass.cast(json);
    }
    
    return DEFAULT_GSON.fromJson(json, targetClass);
  }

  public LinkedHashMap<String, Object> toMap() {
      LinkedHashMap<String, Object> valuesCopy = this.tomlContainer.getValueMap();
    return valuesCopy;
  }

    /**
     *
     * @param values
     * @throws IllegalArgumentException
     */
  public void setValues(Map<String,Object> values) throws IllegalArgumentException{
      try {
          Container.Table table = new Container.Table();
          table.setValueMap(new LinkedHashMap<String, Object>(values));
      } catch (Exception e) {
          throw new IllegalArgumentException(e);
      }
      this.tomlContainer.setValue(new LinkedHashMap<String, Object>(values));
  }

  public void Write(Writer writer) throws IOException {
      new TomlWriter().write(tomlContainer, writer);
  }




  @SuppressWarnings("unchecked")
  private Object get(String key, boolean needConvert) {
      Object current = getInner(key);
      if (needConvert) {
          if (current instanceof Container.Table) {
              Container.Table table = (Container.Table) current;
              current = table.getValueMap();
          } else if (current instanceof Container.TableArray) {
              Container.TableArray tableArray = (Container.TableArray) current;
              current = tableArray.getValueList();
          }
      }
      return current;
  }


    private Object getInner(String key) {
        Map<String, Object> values = tomlContainer.getContainer().getTableValues();
        if (values.containsKey(key)) {
            return values.get(key);
        }

        Object current = tomlContainer.getContainer();

        Keys.Key[] keys = Keys.split(key);

        for (Keys.Key k : keys) {
            if (k.index == -1 && current instanceof Container.Table && ((Container.Table) current).getTableValues().containsKey(k.path)) {
                return ((Container.Table) current).getTableValues().get(k.path);
            }

            current = ((Container.Table) current).getTableValues().get(k.name);

            if (k.index > -1 && current != null) {
                if (k.index >= ((Container.TableArray) current).getTableValue().size()) {
                    return null;
                }

                current = ((Container.TableArray) current).getTableValue().get(k.index);
            }

            if (current == null) {
                return  null;
            }
        }
        return current;
    }
  
  private Toml(Container.Table values) {
     this.tomlContainer = new TomlContainer(values, "");
  }
}
