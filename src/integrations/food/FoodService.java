package integrations.food;

import haven.Defer;
import haven.ItemInfo;
import haven.Resource;
import haven.Utils;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class FoodService {
    private static final String DEFAULT_SERVICE_URL = "https://food.hafen.tech/";
    public static final String API_ENDPOINT = Utils.getpref("food_service_endpoint_api", DEFAULT_SERVICE_URL + "api/");
    private static final String FOOD_DATA_URL = Utils.getpref("food_service_data_url", DEFAULT_SERVICE_URL + "api/data/food-info.json");
    private static final File FOOD_DATA_CACHE_FILE = new File("food_data.json");
    private static String token = "ardClient";  //Config.confid maybe ArdClient also works

    private static final Map<String, ParsedFoodInfo> cachedItems = new ConcurrentHashMap<>();
    private static final Queue<HashedFoodInfo> sendQueue = new ConcurrentLinkedQueue<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    static {
        if (!Resource.language.equals("en")) {
            System.out.println("FoodUtil ERROR: Only English language is allowed to send food data");
        }
        scheduler.execute(FoodService::loadCachedFoodData);
        scheduler.scheduleAtFixedRate(FoodService::sendItems, 10L, 10, TimeUnit.SECONDS);
        //scheduler.scheduleAtFixedRate(FoodService::requestFoodDataCache, 0L, 30, TimeUnit.MINUTES);
    }

    /**
     * Load cached food data from the file (only keys for now since we don't use content anyway)
     */
    private static void loadCachedFoodData() {
        try {
            if (FOOD_DATA_CACHE_FILE.exists()) {
                Files.delete(FOOD_DATA_CACHE_FILE.toPath());
                /*String jsonData = String.join("", Files.readAllLines(FOOD_DATA_CACHE_FILE.toPath(), StandardCharsets.UTF_8));
                JSONObject object = new JSONObject(jsonData);
                object.keySet().forEach(key -> cachedItems.put(key, new ParsedFoodInfo()));
                System.out.println("Loaded food data file: " + cachedItems.size() + " entries");*/
            }
        } catch (Exception ex) {
            System.err.println("Cannot load food data file: " + ex.getMessage());
            try {
                Files.delete(FOOD_DATA_CACHE_FILE.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static double lastModified = -1;

    /**
     * Check last modified for the food_data file and request update from server if too old
     */
    public static void requestFoodDataCache() {
        try {
            /*if (FOOD_DATA_CACHE_FILE.exists()) {
                lastModified = FOOD_DATA_CACHE_FILE.lastModified();
            }*/
            if (lastModified == -1 || (Utils.rtime() - lastModified > 30 * 60)) {//30 * 60 sec
                lastModified = Utils.rtime();//seconds time
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(FOOD_DATA_URL).openConnection();
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setRequestProperty("User-Agent", "H&H Client/" + token);
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    StringBuilder stringBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())))) {
                        stringBuilder.append(reader.readLine());
                    } finally {
                        connection.disconnect();
                    }
                    String content = stringBuilder.toString();

                    /*Files.write(FOOD_DATA_CACHE_FILE.toPath(), Collections.singleton(content), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);*/
                    JSONObject object = new JSONObject(content);
                    //object.keySet().forEach(key -> cachedItems.put(key, new ParsedFoodInfo()));
                    //System.out.println("Updated food data file: " + cachedItems.size() + " entries");
                } catch (Exception ex) {
                    System.err.println("Cannot load remote food data file: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.out.println("Should not happen, but whatever: " + ex.getMessage());
        }
    }

    /**
     * Check item info and determine if it is food and we need to send it
     */
    public static void checkFood(List<ItemInfo> ii, Resource res) {
//        if (!Resource.language.equals("en")) {
//            // Do not process localized items
//            return;
//        }
        List<ItemInfo> infoList = new ArrayList<>(ii);
        Defer.later(() -> {
            try {
                String resName = res.name;
                FoodInfo foodInfo = ItemInfo.find(FoodInfo.class, infoList);
                if (foodInfo != null) {
                    QBuff qBuff = ItemInfo.find(QBuff.class, infoList);
                    double quality = qBuff != null ? qBuff.q : 10.0;
                    double multiplier = Math.sqrt(quality / 10.0);
                    double multiplier2 = Math.sqrt(multiplier);

                    ParsedFoodInfo parsedFoodInfo = new ParsedFoodInfo();
                    parsedFoodInfo.resourceName = resName;
                    parsedFoodInfo.energy = (int) (Math.round(foodInfo.end * 100));
                    parsedFoodInfo.hunger = round2Dig(foodInfo.glut * 1000 / multiplier2);

                    for (int i = 0; i < foodInfo.evs.length; i++) {
                        parsedFoodInfo.feps.add(new FoodFEP(foodInfo.evs[i].ev.orignm, round2Dig(foodInfo.evs[i].a / multiplier)));
                    }

                    for (ItemInfo info : infoList) {
                        if (info instanceof ItemInfo.AdHoc) {
                            String text = ((ItemInfo.AdHoc) info).str.text;
                            // Skip food which base FEP's cannot be calculated
                            if (text.equals("White-truffled")
                                    || text.equals("Black-truffled")
                                    || text.equals("Peppered")) {
                                return (null);
                            }
                        }
                        if (info instanceof ItemInfo.Name) {
                            parsedFoodInfo.itemName = ((ItemInfo.Name) info).ostr.text;
                        }
                        if (info.getClass().getName().equals("Ingredient")) {
                            String name = (String) info.getClass().getField("oname").get(info);
                            Double value = (Double) info.getClass().getField("val").get(info);
                            parsedFoodInfo.ingredients.add(new FoodIngredient(name, (int) (value * 100)));
                        } else if (info.getClass().getName().equals("Smoke")) {
                            String name = (String) info.getClass().getField("oname").get(info);
                            Double value = (Double) info.getClass().getField("val").get(info);
                            parsedFoodInfo.ingredients.add(new FoodIngredient(name, (int) (value * 100)));
                        }
                    }

                    checkAndSend(parsedFoodInfo);
                }
            } catch (Exception ex) {
                System.out.println("Cannot create food info: " + ex.getMessage());
            }
            return (null);
        });
    }

    private static double round2Dig(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void checkAndSend(ParsedFoodInfo info) {
        String hash = generateHash(info);
        if (cachedItems.containsKey(hash)) {
            return;
        }
        sendQueue.add(new HashedFoodInfo(hash, info));
    }

    private static void sendItems() {
        if (sendQueue.isEmpty()) {
            return;
        }

        List<ParsedFoodInfo> toSend = new ArrayList<>();
        while (!sendQueue.isEmpty()) {
            HashedFoodInfo info = sendQueue.poll();
            if (cachedItems.containsKey(info.hash)) {
                continue;
            }
            cachedItems.put(info.hash, info.foodInfo);
            toSend.add(info.foodInfo);
        }

        if (!toSend.isEmpty()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(API_ENDPOINT + "food").openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "H&H Client/" + token);
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(new JSONArray(toSend.toArray()).toString().getBytes(StandardCharsets.UTF_8));
                }
                StringBuilder stringBuilder = new StringBuilder();
                try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    stringBuilder.append(inputStream.readLine());
                }

                int code = connection.getResponseCode();
                if (code != 200) {
                    System.out.println("Response: " + stringBuilder.toString());
                }
                System.out.println("Sent " + toSend.size() + " food items, code: " + code);
            } catch (Exception ex) {
                System.out.println("Cannot send " + toSend.size() + " food items, error: " + ex.getMessage());
            }
        }
    }

    private static String generateHash(ParsedFoodInfo foodInfo) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(foodInfo.itemName).append(";")
                    .append(foodInfo.resourceName).append(";");
            foodInfo.ingredients.forEach(it -> {
                stringBuilder.append(it.name).append(";").append(it.percentage).append(";");
            });

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            return getHex(hash);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot generate food hash");
        }
        return null;
    }

    private static String getHex(byte[] bytes) {
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    private static class HashedFoodInfo {
        public String hash;
        public ParsedFoodInfo foodInfo;

        public HashedFoodInfo(String hash, ParsedFoodInfo foodInfo) {
            this.hash = hash;
            this.foodInfo = foodInfo;
        }
    }

    public static class FoodIngredient {
        private String name;
        private Integer percentage;

        public FoodIngredient(String name, Integer percentage) {
            this.name = name;
            this.percentage = percentage;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, percentage);
        }

        public Integer getPercentage() {
            return percentage;
        }

        public String getName() {
            return name;
        }
    }

    public static class FoodFEP {
        private String name;
        private Double value;

        public FoodFEP(String name, Double value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        public String getName() {
            return name;
        }

        public Double getValue() {
            return value;
        }
    }

    public static class ParsedFoodInfo {
        public String itemName;
        public String resourceName;
        public Integer energy;
        public double hunger;
        public ArrayList<FoodIngredient> ingredients;
        public ArrayList<FoodFEP> feps;

        public ParsedFoodInfo() {
            this.itemName = "";
            this.resourceName = "";
            this.ingredients = new ArrayList<>();
            this.feps = new ArrayList<>();
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemName, resourceName, ingredients);
        }

        public ArrayList<FoodFEP> getFeps() {
            return feps;
        }

        public ArrayList<FoodIngredient> getIngredients() {
            return ingredients;
        }

        public String getItemName() {
            return itemName;
        }

        public String getResourceName() {
            return resourceName;
        }

        public double getHunger() {
            return hunger;
        }

        public Integer getEnergy() {
            return energy;
        }
    }
}
