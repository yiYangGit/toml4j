# toml4j

toml4j is a [TOML 0.4.0](https://github.com/toml-lang/toml/blob/master/versions/en/toml-v0.4.0.md) parser for Java.

this project is fork from (https://github.com/mwanji/toml4j)

For the bleeding-edge version integrating the latest specs, see the [work-in-progress branch](https://github.com/mwanji/toml4j/tree/wip).


```xml

```

Requires Java 1.7 or above.

## Quick start

```java
Toml toml = new Toml().read(getTomlFile());
Map<String, Object> map = toml.toMap();

```

## Usage

A `com.moandjiezana.toml.Toml` instance is populated by calling one of `read(File)`, `read(InputStream)`, `read(Reader)`,  or `read(Toml)`.

```java
Toml toml = new Toml().read("a=1");
```

An exception is thrown if the source is not valid TOML.

The data can then be accessed either by converting the Toml instance to your own class or by accessing tables and keys by name.

### Maps

`Toml#toMap()` is a quick way to turn a Toml instance into a `Map<String, Object>`.

```java
Map<String, Object> map = new Toml().read("a=1").toMap();
```


TOML | Java
---- | ----
Integer | `Long`
Float |  `double` 
String | `String`
Multiline and Literal Strings | `String`
Array | `List`
Table | Custom class, `Map<String, Object>`

Custom classes, Maps and collections thereof can be nested to any level. See [TomlToClassTest#should_convert_fruit_table_array()](src/test/java/com/moandjiezana/toml/TomlToClassTest.java) for an example.

Read and write  configuration support Keep comments finitely

example
```toml
#top comment of title
title = "TOML Example"
#top comment of "sub title"
"sub title" = "Now with quoted keys"
#top comment of database
[database]
  ports = [ 8001, 8001, 8002 ]
  enabled = true
  [database.credentials]
    password = "password"
    
[servers]
  cluster = "hyades"
  [servers.alpha]
  ip = "10.0.0.1"
#top comment of networks
[[networks]] #righ comment of networks
  #top comment of networks.name
  name = "Level 1" #righ comment of networks.name
  [networks.status]
    bandwidth = 10

[[networks]]
  name = "Level 2"

[[networks]]
  name = "Level 3"
  [[networks.operators]]
    location = "Geneva"
  [[networks.operators]]
    location = "Paris"
#top comment of a.b.c.d
[a.b.c.d] #right comment of a.b.c.d

#top comment of arrayCommentTest  (if comment of tableArray.length change all comment will clean)
[[arrayCommentTest]] #right comment of arrayCommentTest
name = 1

 ## last comment of this toml config
```

```java
Toml toml = new Toml();
toml.read(inputReader());
Map<String, Object> map = toml.toMap();
map.put("title", "TOML Example-replace");
map.remove("sub title");
List<Map<String, Object>> arrayCommentTest = (List<Map<String, Object>>) map.get("arrayCommentTest");
HashMap<String, Object> e = new HashMap<>();
e.put("name", 2);
arrayCommentTest.add(e);
toml.setValues(map);
toml.Write(outPutWriter());
```
then outPut is 
```toml
#top comment of title
title = "TOML Example-replace"
#top comment of database
[database]
  ports = [8001, 8001, 8002]
  enabled = true
  [database.credentials]
    password = "password"
    
[servers]
  cluster = "hyades"
  [servers.alpha]
  ip = "10.0.0.1"
#top comment of networks
[[networks]] #righ comment of networks
  #top comment of networks.name
  name = "Level 1" #righ comment of networks.name
  [networks.status]
    bandwidth = 10

[[networks]]
  name = "Level 2"

[[networks]]
  name = "Level 3"
  [[networks.operators]]
    location = "Geneva"
  [[networks.operators]]
    location = "Paris"
[a]
[a.b]
[a.b.c]
#top comment of a.b.c.d
[a.b.c.d] #right comment of a.b.c.d
[[arrayCommentTest]]
name = 1
[[arrayCommentTest]]
name = 2

 ## last comment of this toml config
```

### Limitations

Date precision is limited to milliseconds.

## Changelog

Please see the [changelog](CHANGELOG.md).


## License

toml4j is copyright (c) 2013-2015 Moandji Ezana and is licensed under the [MIT License](LICENSE)
