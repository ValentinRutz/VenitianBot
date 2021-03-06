package utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import play.Logger;
import tweets.Location;
import tweets.LocationBox;
import tweets.Place;
import tweets.Response;

import java.io.*;
import java.util.*;

public class Utilities {

    public static String consHashtags(String keywords) {
        if (keywords != null && !" ".equals(keywords)) {
            return keywords.replaceAll(" ", " #");
        }
        return keywords;
    }

    public static String[] readKeywords() {
        return readJSON("./app/assets/query.json", "hashtags");
    }

    public static String[] readKeywordsGeneral() {
        return readJSON("./app/assets/query.json", "hashtags-general");
    }

    public static String[] readKeywordsBlackList() {
        return readJSON("./app/assets/query.json", "black-list");
    }

    public static ArrayList<Long> readFeaturedUsers() {
        String[] featured =  readJSON("./app/assets/query.json", "featured-users");

        ArrayList<Long>  featuredIds =new ArrayList<>();

        for (String id: featured) {
            featuredIds.add(Long.parseLong(id));
        }

        return featuredIds;
    }

    private static String[] readJSON(String filePath, String key) {
        JSONParser parser = new JSONParser();
        FileReader queryFile = null;
        try {
            queryFile = new FileReader(filePath);
            JSONObject queryJSON = (JSONObject) parser.parse(queryFile);
            JSONArray values = (JSONArray) queryJSON.get(key);

            String[] hashtagsArray = new String[values.size()];

            for (int i = 0; i < hashtagsArray.length; ++i) {
                hashtagsArray[i] = "" + values.get(i);
            }

            return hashtagsArray;
        } catch (Exception e) {
            Logger.error(e.toString());
        } finally {
            if (queryFile != null) {
                try {
                    queryFile.close();
                } catch (IOException e) {
                    Logger.error(e.toString());
                }
            }
        }
        return new String[]{};
    }

    public static LocationBox readGeoLocation() {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        try {
            fr = new FileReader("./app/assets/query.json");
            JSONObject queryJSON = (JSONObject) parser.parse(fr);
            Location veniceSW = getLocationByName(queryJSON, "veniceSW");
            Location veniceNE = getLocationByName(queryJSON, "veniceNE");
            return new LocationBox(veniceSW, veniceNE);
        } catch (Exception e) {
            Logger.error(e.toString());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Logger.error(e.toString());
                }
            }
        }
        return null;
    }

    private static Location getLocationByName(JSONObject json,
                                              String locationName) {
        JSONObject location = (JSONObject) json.get(locationName);

        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");
        double radius = (double) location.get("radius");

        return new Location(locationName, latitude, longitude, radius);
    }

    public static Place[] readMonumentsLocation() {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        Place[] monuments;
        try {
            fr = new FileReader("./app/assets/monuments_location.json");
            JSONObject placesJSON = (JSONObject) parser.parse(fr);

            JSONArray places = (JSONArray) placesJSON.get("places");
            String[] placesName = new String[places.size()];

            for (int i = 0; i < placesName.length; ++i) {
                placesName[i] = (String) places.get(i);
            }

            monuments = new Place[placesName.length];
            for (int i = 0; i < placesName.length; ++i) {
                String placeName = placesName[i];
                JSONObject place = (JSONObject) placesJSON.get(placeName);

                JSONArray tags = (JSONArray) place.get("tags");
                HashSet<String> tagSet = new HashSet<>();

                for (Object tag : tags) {
                    tagSet.add((String) tag);
                }

                double latitude = (double) place.get("latitude");
                double longitude = (double) place.get("longitude");
                double radius = (double) place.get("radius");
                monuments[i] = new Place(placeName, tagSet, latitude,
                        longitude, radius);
            }
            return monuments;
        } catch (IOException | ParseException e) {
            Logger.error(e.toString());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Logger.error(e.toString());
                }
            }
        }
        return null;
    }

    public static String[] getMonumentsKeyword(Location[] locations) {
        String[] monumentsKeyword = new String[locations.length];
        for (int i = 0; i < locations.length; ++i) {
            monumentsKeyword[i] = locations[i].getName();
        }
        return monumentsKeyword;
    }

    public static List<Response> readResponses() {
       return readResponses("./app/assets/responses.json");
    }

    public static List<Response> readStatusUpdates() {
        return readResponses("./app/assets/statusupdates.json");
    }
    public static List<Response> readResponses(String fileName) {
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        List<Response> responses = new ArrayList<>();
        try {
            fr = new FileReader(fileName);
            JSONArray respJSON = (JSONArray) parser.parse(fr);

            for (int i = 0; i < respJSON.size(); ++i) {
                JSONObject value = (JSONObject) respJSON.get(i);

                JSONArray tagsArray = (JSONArray) value.get("tags");

                Set<String> tags = new HashSet<>();

                for (int j = 0; j < tagsArray.size(); ++j) {
                    tags.add((String) tagsArray.get(j));
                }

                responses.add(new Response((String) value.get("tweet"), tags));
            }

            return responses;
        } catch (ParseException | IOException e) {
            Logger.error(e.toString());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Logger.error(e.toString());
                }
            }
        }
        return null;
    }

    public static void writeResponse(Response response) {
        writeResponsesToFile(response, "./app/assets/responses.json");

    }

    public static void writeStatusUpdate(Response status) {
        writeResponsesToFile(status, "./app/assets/statusupdates.json");
    }

    private static void writeResponsesToFile(Response response, String fileName) {
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(fileName, "rw");
            file.seek(file.length() - 2);
            file.writeBytes(",\n\t{\n\t\t\"tweet\": \"");
            file.writeBytes(response.getTweet());
            file.writeBytes("\",\n\t\t\"tags\": [ ");
            Iterator<String> tags = response.getTags().iterator();
            if (tags.hasNext()) {
                file.writeBytes("\"" + tags.next() + "\"");
            }
            while (tags.hasNext()) {
                file.writeBytes(", ");
                file.writeBytes("\"" + tags.next() + "\"");
            }
            file.writeBytes(" ]\n\t}\n]");
        } catch (IOException e) {
            Logger.error(e.toString());
        }
    }

    public static HashMap<Long, Long> readTweetedUsers(String filePath) {
        HashMap<Long, Long> users = new HashMap<>();
        String line;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                users.put(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]));
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filePath + " not found.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
        }

        return users;
    }

    public static void writeTweetedUsers(HashMap<Long, Long> users, String filePath) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(filePath, false);
            Set<Long> keySet = users.keySet();
            for (Long key : keySet) {
                writer.append(key + "," + users.get(key) + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.getStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
        }
    }

    public static Set<String> extractTags(String tweet) {
        Set<String> tags = new HashSet<>();

        for (String keyWord : JSONReader.keyWords) {
            if (tweet.toLowerCase().replaceAll(" ", "").contains(keyWord)) {
                tags.add(keyWord);
            }
        }

        for (String keyWord : JSONReader.keyWordsGeneral) {
            if (tweet.toLowerCase().replaceAll(" ", "").contains(keyWord)) {
                tags.add(keyWord);
            }
        }

        return tags;
    }

}
