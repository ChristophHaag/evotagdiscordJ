import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class WC3stats {
    boolean use_cached_json;
    int cached_json_num = -1;

    WC3stats(boolean use_cached_json) {
        this.use_cached_json = use_cached_json;
    }

    String get_html() {
        if (this.use_cached_json) {
            return CachedJson.get_json(++cached_json_num % CachedJson.count);
        }

        try {
            URL url = new URL("https://api.wc3stats.com/gamelist");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            StringBuffer content = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    ArrayList<WarcraftGameInfo> get_evo_tag_games() {
        String html = get_html();

        //System.out.println(html);

        Gson g = new Gson();
        WarcraftGameList list = g.fromJson(html, WarcraftGameList.class);

        System.out.println("Parsed list, status " + list.status);

        // usually just 1 game
        ArrayList<WarcraftGameInfo> evo_tag_games = new ArrayList<>(1);
        for (WarcraftGameInfo info : list.body) {
            String map = info.map.toLowerCase();
            if (map.startsWith("evolution tag")) {
                //System.out.println("Found map: " + info.map);
                evo_tag_games.add(info);
            }
        }
        return evo_tag_games;
    }

}
