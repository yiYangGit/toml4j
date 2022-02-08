package com.moandjiezana.toml;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.moandjiezana.toml.testutils.Utils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.*;
import java.util.*;

/**
 * Created by yangyi0 on 2021/2/25.
 */
public class TestTomlContainer {
    private static final Gson GSON = new Gson();
    @Test()
    public void testTomlContainerWrite() throws Exception {
        File file = Utils.file(getClass(), "example");
        Set<String> testIgnoreFile = new HashSet<>();
        testIgnoreFile.add("hard_example_errors.toml");
        List<File> tomlFiles = new ArrayList<>();
        for (File tomlFile : file.getParentFile().listFiles()) {
            String name = tomlFile.getName();
            if (name.endsWith(".toml")&&!testIgnoreFile.contains(name)) {
                tomlFiles.add(tomlFile);
            }
        }
        for (File tomlFile : tomlFiles) {
            Toml toml1 = new Toml();
            Toml toml2 = new Toml();
            Toml toml4 = new Toml();
            InputStreamReader reader = createReader(tomlFile);
            Map<String, Object> map1 = toml2.read(reader).toMap();
            TomlWriter tomlWriter2 = new TomlWriter();
            String tomStr4 = tomlWriter2.write(map1);
            Map<String, Object> toml4Map = toml4.read(tomStr4).toMap();
            InputStreamReader reader2 = createReader(tomlFile);
            TomlContainer tomlContainer = toml2.readerToTomlContainer(reader2);
            TomlWriter tomlWriter = new TomlWriter();
            StringWriter target = new StringWriter();
            tomlWriter.write(tomlContainer, target);
            Map<String, Object> map2 = toml1.read(target.toString()).toMap();
            TomlContainer tomlContainer1 = new TomlContainer(new Container.Table(), "\n #nothing|");
            tomlContainer1.setValue(new LinkedHashMap<String, Object>(map2));
            StringWriter target1 = new StringWriter();
            tomlWriter.write(tomlContainer1, target1);
            Map<String, Object> map3 = toml1.read(target1.toString()).toMap();
            JsonElement map1Tree = GSON.toJsonTree(map1);
            assertEquals(GSON.toJsonTree(map3), map1Tree);
            assertEquals(GSON.toJsonTree(toml4Map), map1Tree);
            assertEquals(map1Tree, GSON.toJsonTree(map2));
        }
    }
    @Test()
    public  void testMapReadWrite() throws IOException {
        Toml toml = new Toml();
        toml.read(new StringReader("#top comment of title\n" +
                "title = \"TOML Example\"\n" +
                "#top comment of \"sub title\"\n" +
                "\"sub title\" = \"Now with quoted keys\"\n" +
                "#top comment of database\n" +
                "[database]\n" +
                "  ports = [ 8001, 8001, 8002 ]\n" +
                "  enabled = true\n" +
                "  [database.credentials]\n" +
                "    password = \"password\"\n" +
                "    \n" +
                "[servers]\n" +
                "  cluster = \"hyades\"\n" +
                "  [servers.alpha]\n" +
                "  ip = \"10.0.0.1\"\n" +
                "#top comment of networks\n" +
                "[[networks]] #righ comment of networks\n" +
                "  #top comment of networks.name\n" +
                "  name = \"Level 1\" #righ comment of networks.name\n" +
                "  [networks.status]\n" +
                "    bandwidth = 10\n" +
                "\n" +
                "[[networks]]\n" +
                "  name = \"Level 2\"\n" +
                "\n" +
                "[[networks]]\n" +
                "  name = \"Level 3\"\n" +
                "  [[networks.operators]]\n" +
                "    location = \"Geneva\"\n" +
                "  [[networks.operators]]\n" +
                "    location = \"Paris\"\n" +
                "#top comment of a.b.c.d\n" +
                "[a.b.c.d] #right comment of a.b.c.d\n" +
                "\n" +
                "#top comment of arrayCommentTest  (if comment of tableArray.length change all comment will clean)\n" +
                "[[arrayCommentTest]] #right comment of arrayCommentTest\n" +
                "name = 1\n" +
                "\n" +
                " ## last comment of this toml config"));
        Map<String, Object> map = toml.toMap();
        map.put("title", "TOML Example-replace");
        map.remove("sub title");
        List<Map<String, Object>> arrayCommentTest = (List<Map<String, Object>>) map.get("arrayCommentTest");
        HashMap<String, Object> e = new HashMap<>();
        e.put("name", 2);
        arrayCommentTest.add(e);
        toml.setValues(map);
        StringWriter writer = new StringWriter();
        toml.Write(writer);
        toml.read(new StringReader(writer.toString()));
        map = toml.toMap();
        assertNull(map.get("name"));
        List<Map<String,Object>> list = (List) map.get("arrayCommentTest");
        assertEquals(2, list.size());
        Object name = list.get(1).get("name");
        assertEquals(2L, name);
        assertEquals(toml.getString("database.credentials.password"), "password");
        assertEquals(toml.getString("networks[2].operators[0].location"), "Geneva");
    }


    @Test()
    public void testDoubleReadWrite() throws Exception {
        Random random = new Random();
        Map<String, Object> value = new HashMap<>();
        String key = "double";
        for (int i = 0; i < 5000; i++) {
            double d = random.nextDouble();
            if (i%2==0) {
                d = d * random.nextDouble();
            }
            if (i%4==0) {
                d = d * random.nextLong();
            }
            if (i%8==0) {
                d = d * random.nextFloat();
            }
            d = d * 2;
            d = Math.pow(d, random.nextDouble());
            if (i%16>12) {
                d = Double.NaN;
                if (i%16==13) {
                    d = i * 1.0;
                }
                if (i%16==14) {
                    d = Double.POSITIVE_INFINITY;
                }
                if (i%16==15) {
                    d = Double.NEGATIVE_INFINITY;
                }
            }

            value.put(key, d);
            Toml toml = new Toml();
            toml.setValues(value);
            StringWriter writer = new StringWriter();
            toml.Write(writer);
            String s = writer.toString();
            toml.read(new StringReader(s));;
            assertEquals(new Double(d), toml.getDouble(key));
        }
    }

    private InputStreamReader createReader(File tomlFile) throws UnsupportedEncodingException, FileNotFoundException {
        return new InputStreamReader(new FileInputStream(tomlFile), "UTF8");
    }


}
