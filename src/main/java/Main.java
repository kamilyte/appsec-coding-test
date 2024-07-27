import java.io.*;
import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonElement;


public class Main {
    // linked hashmap to preserve insertion order
    private static final Map<String, String> replacementMap = new LinkedHashMap<>();

    // variable for the chosen format of the replacement map
    private static final String formatString = "\\x%02x";

    // variable for the output file name of the obfuscated JSON file
    private static final String outputFileName = "output.json";

    // variable for the output file name of the replacement map file
    private static final String mapFileName = "replacementMap.txt";


    public static void main(String[] args) throws IOException {

        // parse JSON file into a JsonObject then obfuscate the JsonObject
        JsonObject jsonObject = JsonParser.parseReader(new FileReader("src/main/java/input.json")).getAsJsonObject();
        JsonElement obfuscatedJson = obfuscate(jsonObject);

        // replace the double escaped sequences with single backslashes
        String jsonString = String.valueOf(obfuscatedJson).replace("\\\\u", "\\u");

        // write json string to file
        try (FileWriter writer = new FileWriter("src/main/java/" + outputFileName)) {
            writer.write(jsonString);
        }

        // write replacement map to file
        writeToReplacementMapFile();
    }

    // write replacement mapping to file
    public static void writeToReplacementMapFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/" + mapFileName))) {
            for (Map.Entry<String, String> pair : replacementMap.entrySet()) {
                writer.write(pair.getKey() + " -> " + pair.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // recursive obfuscation function depending on type of JsonElement
    public static JsonElement obfuscate(JsonElement json) {
        if (json instanceof JsonObject) {
            JsonObject obfuscatedJson = new JsonObject();
            ((JsonObject) json).keySet().forEach(key -> {
                // add key to replacement map
                replacementMap.put(key, formattedString(key));
                // add obfuscated JSON pairs
                obfuscatedJson.add(obfuscateString(key), obfuscate(((JsonObject) json).get(key)));
            });
            // return final obfuscated Json object
            return obfuscatedJson;
        } else if (json instanceof JsonArray) {
            JsonArray obfuscatedJsonArray = new JsonArray();
            // iterates through the array Json elements
            ((JsonArray) json).forEach(item -> obfuscatedJsonArray.add(obfuscate(item)));
            // return obfuscated array to be added to the obfuscated Json object
            return obfuscatedJsonArray;
        } else if (json instanceof JsonPrimitive) {
            // only obfuscate if the Json primitive is a string
            if (((JsonPrimitive) json).isString()) {
                String str = json.getAsString();
                // add primitive to replacement map
                replacementMap.put(str, formattedString(str));
                // return obfuscated Json primitive
                return new JsonPrimitive(obfuscateString(str));
            }
        }
        // if not a Json element or string, returns the integer or boolean value without obfuscation
        return json;
    }

    // obfuscates string to unicode format by splitting to chars and concatenating each obfuscated character
    public static String obfuscateString(String str) {
        return str.chars().mapToObj(chr -> String.format("\\u%04x", chr)).reduce("", (obfStr, c) -> obfStr + c);
    }

    // formats string to chosen format by splitting to chars and concatenating each formatted character
    public static String formattedString(String str) {
        return str.chars().mapToObj(chr -> String.format(formatString, chr)).reduce("", (obfStr, c) -> obfStr + c);
    }

}
