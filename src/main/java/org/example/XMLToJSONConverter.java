package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class XMLToJSONConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java XMLToJSONConverter <task> <xmlFile> [additional arguments]");
            System.out.println("Tasks:");
            System.out.println("  1: Convert XML to JSON");
            System.out.println("  2: Extract sub-object using JSONPointer");
            System.out.println("  3: Check if XML has key path");
            System.out.println("  4: Add prefix to all keys");
            System.out.println("  5: Replace sub-object at path");
            return;
        }

        String task = args[0];
        String xmlFilePath = args[1];

        try {
            switch (task) {
                case "1":
                    convertXmlToJson(xmlFilePath);
                    break;
                case "2":
                    if (args.length < 3) {
                        System.out.println("Task 2 requires a JSONPointer path as third argument");
                        return;
                    }
                    extractSubObject(xmlFilePath, args[2]);
                    break;
                case "3":
                    if (args.length < 3) {
                        System.out.println("Task 3 requires a key path as third argument");
                        return;
                    }
                    checkKeyPath(xmlFilePath, args[2]);
                    break;
                case "4":
                    addPrefixToKeys(xmlFilePath);
                    break;
                case "5":
                    if (args.length < 3) {
                        System.out.println("Task 5 requires a key path as third argument");
                        return;
                    }
                    replaceSubObject(xmlFilePath, args[2]);
                    break;
                default:
                    System.out.println("Unknown task: " + task);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void convertXmlToJson(String xmlFilePath) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting conversion of " + xmlFilePath + " to JSON...");
        
        File xmlFile = new File(xmlFilePath);
        String outputPath = xmlFilePath.replace(".xml", ".json");
        
        try (FileInputStream fis = new FileInputStream(xmlFile)) {
            JSONObject jsonObject = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(xmlFilePath))));
            
            try (FileWriter fileWriter = new FileWriter(outputPath)) {
                fileWriter.write(jsonObject.toString(2)); // Pretty print with 2 space indentation
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("Conversion completed in " + (endTime - startTime) + "ms");
            System.out.println("JSON saved to " + outputPath);
        }
    }

    private static void extractSubObject(String xmlFilePath, String jsonPointerPath) throws IOException {
        System.out.println("Extracting sub-object from " + xmlFilePath + " using path: " + jsonPointerPath);
        
        JSONObject jsonObject = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(xmlFilePath))));
        
        try {
            Object subObject = jsonObject.query(jsonPointerPath);
            if (subObject == null) {
                System.out.println("Path not found in the JSON object");
                return;
            }
            
            String outputPath = xmlFilePath.replace(".xml", "_sub.json");
            try (FileWriter fileWriter = new FileWriter(outputPath)) {
                if (subObject instanceof JSONObject) {
                    fileWriter.write(((JSONObject) subObject).toString(2));
                } else if (subObject instanceof JSONArray) {
                    fileWriter.write(((JSONArray) subObject).toString(2));
                } else {
                    // If the result is not a JSONObject or JSONArray, wrap it
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("result", subObject);
                    fileWriter.write(wrapper.toString(2));
                }
            }
            
            System.out.println("Sub-object extracted and saved to " + outputPath);
        } catch (Exception e) {
            System.err.println("Error extracting sub-object: " + e.getMessage());
        }
    }

    private static void checkKeyPath(String xmlFilePath, String keyPath) throws IOException {
        System.out.println("Checking if " + xmlFilePath + " has key path: " + keyPath);
        
        JSONObject jsonObject = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(xmlFilePath))));
        
        boolean hasKeyPath = false;
        try {
            Object result = jsonObject.query(keyPath);
            hasKeyPath = (result != null);
        } catch (Exception e) {
            // If an exception occurs, the path doesn't exist
        }
        
        if (hasKeyPath) {
            System.out.println("Key path found. Saving JSON to disk...");
            String outputPath = xmlFilePath.replace(".xml", "_keypath.json");
            try (FileWriter fileWriter = new FileWriter(outputPath)) {
                fileWriter.write(jsonObject.toString(2));
            }
            System.out.println("JSON saved to " + outputPath);
        } else {
            System.out.println("Key path not found. Discarding JSON object.");
        }
    }

    private static void addPrefixToKeys(String xmlFilePath) throws IOException {
        System.out.println("Adding prefix 'swe262_' to all keys in " + xmlFilePath);
        
        JSONObject jsonObject = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(xmlFilePath))));
        JSONObject prefixedJsonObject = addPrefixToKeysRecursive(jsonObject, "swe262_");
        
        String outputPath = xmlFilePath.replace(".xml", "_prefixed.json");
        try (FileWriter fileWriter = new FileWriter(outputPath)) {
            fileWriter.write(prefixedJsonObject.toString(2));
        }
        
        System.out.println("Prefixed JSON saved to " + outputPath);
    }

    private static JSONObject addPrefixToKeysRecursive(JSONObject jsonObject, String prefix) {
        JSONObject result = new JSONObject();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            String newKey = prefix + key;
            
            if (value instanceof JSONObject) {
                result.put(newKey, addPrefixToKeysRecursive((JSONObject) value, prefix));
            } else if (value instanceof JSONArray) {
                result.put(newKey, addPrefixToKeysArray((JSONArray) value, prefix));
            } else {
                result.put(newKey, value);
            }
        }
        return result;
    }
    
    private static JSONArray addPrefixToKeysArray(JSONArray jsonArray, String prefix) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                result.put(addPrefixToKeysRecursive((JSONObject) value, prefix));
            } else if (value instanceof JSONArray) {
                result.put(addPrefixToKeysArray((JSONArray) value, prefix));
            } else {
                result.put(value);
            }
        }
        return result;
    }

    private static void replaceSubObject(String xmlFilePath, String keyPath) throws IOException {
        System.out.println("Replacing sub-object at path " + keyPath + " in " + xmlFilePath);

        // 1. 读取XML并转换为JSONObject
        JSONObject jsonObject = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(xmlFilePath))));

        // 2. 创建替换对象
        JSONObject replacementObject = new JSONObject();
        replacementObject.put("replacedBy", "XMLToJSONConverter");
        replacementObject.put("timestamp", System.currentTimeMillis());
        replacementObject.put("message", "This sub-object was replaced");

        // 3. 分割路径（如 ["", "catalog", "book", "2"]）
        String[] pathSegments = keyPath.split("/");
        if (pathSegments.length <= 1) {
            System.out.println("Invalid path format. Path should be in the format /key1/key2/...");
            return;
        }

        // 4. 导航到目标位置的父对象
        Object current = jsonObject; // 改为Object类型以兼容JSONArray
        for (int i = 1; i < pathSegments.length - 1; i++) {
            String segment = pathSegments[i];

            if (current instanceof JSONObject) {
                JSONObject currentObj = (JSONObject) current;
                if (!currentObj.has(segment)) {
                    System.out.println("Path segment not found: " + segment);
                    return;
                }
                current = currentObj.get(segment);
            } else {
                System.out.println("Path segment is not navigable: " + segment);
                return;
            }
        }

        // 5. 执行替换
        String lastSegment = pathSegments[pathSegments.length - 1];
        try {
            if (current instanceof JSONObject) {
                JSONObject parentObj = (JSONObject) current;
                if (!parentObj.has(lastSegment)) {
                    System.out.println("Target key not found: " + lastSegment);
                    return;
                }
                parentObj.put(lastSegment, replacementObject);
            }
            // 处理数组索引（如 /catalog/book/2）
            else if (current instanceof JSONArray) {
                JSONArray array = (JSONArray) current;
                int index;
                try {
                    index = Integer.parseInt(lastSegment);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid array index: " + lastSegment);
                    return;
                }
                if (index < 0 || index >= array.length()) {
                    System.out.println("Array index out of bounds: " + index);
                    return;
                }
                array.put(index, replacementObject);
            } else {
                System.out.println("Cannot replace non-object/non-array value at path: " + keyPath);
                return;
            }
        } catch (Exception e) {
            System.out.println("Replacement failed: " + e.getMessage());
            return;
        }

        // 6. 保存结果
        String outputPath = xmlFilePath.replace(".xml", "_replaced.json");
        try (FileWriter fileWriter = new FileWriter(outputPath)) {
            fileWriter.write(jsonObject.toString(2)); // 缩进2个空格美化输出
            System.out.println("JSON with replaced sub-object saved to " + outputPath);
        }
    }
}
