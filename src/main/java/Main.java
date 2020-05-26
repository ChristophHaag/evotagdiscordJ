import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.*;

class DiscordGameInfo {
    WarcraftGameInfo info = null;
    CompletableFuture<Message> msg_future = null;
    private Message msg = null;

    Message get_message() {
        if (msg_future == null) {
            System.out.println("Message future not set!");
            return null;
        }

        if (msg_future.isDone()) {
            msg = msg_future.getNow(null);

            if (msg == null) {
                System.out.println("Message future done but message still null?!");
            }

            return msg;
        } else {
            System.out.println("Message not done yet!");
            return null;
        }
    }
}

public class Main {
    int period = 10; // seconds
    boolean use_cached_json = false;
    boolean online = true;
    boolean use_test_channel = false;

    Server server;
    ServerTextChannel channel;

    Map<Integer, DiscordGameInfo> current_games = new ConcurrentHashMap<>();

    DiscordApi api;
    WC3stats wc3stats;

    Main(String token) {
        if (online) {
            api = new DiscordApiBuilder().setToken(token).login().join();
            System.out.println("Logged in!");
        } else {
            System.out.println("offline mode");
        }
    }

    String game_info_to_string(WarcraftGameInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(info.name);
        sb.append(" [");
        sb.append(String.valueOf(info.slotsTaken));
        sb.append("/");
        sb.append(String.valueOf(info.slotsTotal));
        sb.append("] - ");
        sb.append(info.map);
        sb.append(" Hosted by [");
        sb.append(info.host);
        sb.append("] on Server [");
        sb.append(info.server);
        sb.append("]");

        return sb.toString();
    }

    void update_games() {
        ArrayList<WarcraftGameInfo> wc3stats_games = wc3stats.get_evo_tag_games();
        if (wc3stats_games == null) {
            System.out.println("Failed to update games");
            return;
        }

        for (Integer key : current_games.keySet()) {
            boolean still_current = false;
            for (WarcraftGameInfo wc3stats_game : wc3stats_games) {
                if (key.equals(wc3stats_game.id)) {
                    still_current = true;
                }
            }
            if (!still_current) {
                DiscordGameInfo dgi = current_games.get(key);

                Message msg = dgi.get_message();
                if (msg == null) {
                    System.out.println("No message for started or cancelled game...");
                    if (dgi.msg_future != null) {
                        dgi.msg_future.cancel(true);
                    }
                }
                String prev_msg_str = msg.getContent();
                String new_msg = "~~" + prev_msg_str + "~~";
                System.out.println("Editing\n\t" + prev_msg_str + "\n\t" + new_msg);
                msg.edit(new_msg);
                current_games.remove(key);
            }
        }

        for (WarcraftGameInfo info : wc3stats_games) {

            if (current_games.containsKey(info.id)) {
                DiscordGameInfo dgi = current_games.get(info.id);
                dgi.info = info;

                Message msg = dgi.get_message();

                if (msg == null) {
                    System.out.println("No message");
                    continue;
                }

                String prev_msg_str = msg.getContent();
                String new_msg = game_info_to_string(info);
                if (!prev_msg_str.equals(new_msg)) {
                    System.out.println("Editing\n\t" + prev_msg_str + "\n\t" + new_msg);
                    msg.edit(new_msg);
                }
            } else {
                DiscordGameInfo dgi = new DiscordGameInfo();

                String game_str = game_info_to_string(info);
                System.out.println("New game: " + game_str);
                CompletableFuture<Message> message = channel.sendMessage(game_str);

                dgi.info = info;
                dgi.msg_future = message;

                current_games.put(info.id, dgi);
            }

        }
    }

    void run_bot() {

        Collection<Server> servers = api.getServersByName("Evo Tag");
        if (servers.size() == 1) {
            server = servers.iterator().next();
            System.out.println("Using server " + server.toString());
        } else {
            System.out.println("Use a more specific server name!");
            return;
        }

        String channel_name = "hosted-games";
        if (use_test_channel)
            channel_name = "bot-test";

        List<ServerChannel> channels = server.getChannelsByName(channel_name);
        if (channels.size() == 1) {
            channel = (ServerTextChannel) channels.get(0);
            System.out.println("Using channel " + channel.toString());
        } else {
            System.out.println("Use a more specific channel name!");
            return;
        }

        this.wc3stats = new WC3stats(use_cached_json);

        if (online) {
            // Add a listener which answers with "Pong!" if someone writes "!ping"
            api.addMessageCreateListener(event -> {
                if (event.getMessageContent().equalsIgnoreCase("!ping")) {
                    event.getChannel().sendMessage("Pong!");
                }
            });
        }


        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                update_games();
            }
        }, 0, period * 1000);
    }



    public static void main(String[] args) {
        // Insert your bot's token here
        String token = "";
        if (args.length > 0) {
            try {
                Scanner in = new Scanner(new FileReader(args[0]));
                StringBuilder sb = new StringBuilder();
                while (in.hasNext()) {
                    String next = in.next();
                    if (Character.isLetterOrDigit(next.charAt(0))) {
                        sb.append(next);
                    } else {
                        break;
                    }
                }
                token = sb.toString();
                System.out.println("Read Token: " + token);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Argument 1 must be text file containing discord token");
            return;
        }

        Main m = new Main(token);
        m.run_bot();
    }

}
