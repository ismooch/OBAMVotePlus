package com.obam.ismooch.obamvote;

import co.obam.ismooch.obamapi.ObamAPI;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Created by troyj_000 on 3/1/2015.
 */
public class OBAMVotePlus extends JavaPlugin implements Listener {

    public static Map<String, String> services = new HashMap<String, String>();
    public static List<String> serviceList = new ArrayList<String>();

    public static void setVote(UUID uuid, String service, Timestamp time) {


        String stringUUID = uuid.toString();
        ObamAPI.openConnection();
        try {

            service = service.replace(".", "");
            service = service.replace("-", "");

            PreparedStatement sql = ObamAPI.connection.prepareStatement("UPDATE Vote_Times SET " + service + " = ? WHERE UUID = ?");
            sql.setTimestamp(1, time);
            sql.setString(2, stringUUID);
            sql.executeUpdate();
            sql.close();
            ObamAPI.closeConnection();
            return;

        } catch (SQLException e) {

            e.printStackTrace();
            ObamAPI.closeConnection();
            return;
        }

    }

    public static Timestamp getLastVote(UUID uuid, String service) {

        String stringUUID = uuid.toString();
        ObamAPI.openConnection();
        try {
            PreparedStatement sql = ObamAPI.connection.prepareStatement("SELECT * FROM Vote_Times WHERE uuid = ?");
            sql.setString(1, stringUUID);
            ResultSet rs = sql.executeQuery();
            if (rs.next()) {

                Timestamp time = rs.getTimestamp(service);
                sql.close();
                ObamAPI.closeConnection();
                return time;

            } else {

                PreparedStatement newVote = ObamAPI.connection.prepareStatement("INSERT INTO Vote_Times (UUID) VALUES (?)");
                newVote.setString(1, stringUUID);
                newVote.executeUpdate();
                newVote.close();
                ObamAPI.closeConnection();
                sql.close();
                return null;
            }

        } catch (SQLException e) {

            e.printStackTrace();
            ObamAPI.closeConnection();
            return null;
        }


    }

    public static Map<TimeUnit, Long> computeDiff(Date date1, Date date2) {
        long diffInMillies = date2.getTime() - date1.getTime();
        List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);

        Map<TimeUnit, Long> result = new LinkedHashMap<TimeUnit, Long>();
        long milliesRest = diffInMillies;
        for (TimeUnit unit : units) {
            long diff = unit.convert(milliesRest, TimeUnit.MILLISECONDS);
            long diffInMilliesForUnit = unit.toMillis(diff);
            milliesRest = milliesRest - diffInMilliesForUnit;
            result.put(unit, diff);
        }
        return result;
    }

    @Override
    public void onEnable() {
        new BungeeMessenger(this);
        getServer().getPluginManager().registerEvents(this, this);
        serviceList.clear();
        services.clear();
        serviceList = getServiceList();
        services = getServices();
    }

    private List<String> getServiceList() {

        ObamAPI.openConnection();
        try {

            PreparedStatement sql = ObamAPI.connection.prepareStatement("SELECT * FROM Service_List");
            ResultSet rs = sql.executeQuery();
            List<String> list = new ArrayList<String>();
            while (rs.next()) {

                list.add(rs.getString("Service"));

            }

            return list;

        } catch (SQLException e) {

            e.printStackTrace();
            ObamAPI.closeConnection();
            return null;
        }
    }

    private Map<String, String> getServices() {

        ObamAPI.openConnection();
        try {

            PreparedStatement sql = ObamAPI.connection.prepareStatement("SELECT * FROM Service_List");
            ResultSet rs = sql.executeQuery();
            Map<String, String> list = new HashMap<String, String>();
            while (rs.next()) {

                list.put(rs.getString("Service"), rs.getString("URL"));

            }

            return list;

        } catch (SQLException e) {

            e.printStackTrace();
            ObamAPI.closeConnection();
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("vote")) {

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            Date date = new Date();
            Timestamp now = new Timestamp(date.getTime());
            player.sendMessage(" ");
            player.sendMessage(ChatColor.GRAY + "Voting for " + ChatColor.GOLD + "OBAM" + ChatColor.WHITE + "Craft" + ChatColor.GRAY + " not only helps the server in a big way, but is also a great way to" + ChatColor.GRAY + " earn special rewards, including " + ChatColor.YELLOW + "Tickets" + ChatColor.GRAY + ", " + ChatColor.YELLOW + "OBucks" + ChatColor.GRAY + ", and other " + ChatColor.DARK_AQUA + "Custom Items" + ChatColor.GRAY + "!");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Stub Balance: " + ChatColor.WHITE + String.valueOf(ObamAPI.getStubs(ObamAPI.getUUID(player.getName()))));
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "Visit " + ChatColor.GREEN + "http://obam.co/vote " + ChatColor.WHITE + "for more info!");
            player.sendMessage("");

            for (String service : serviceList) {

                String servicename = service.replace(".", "");
                servicename = servicename.replace("-", "");
                Timestamp check = getLastVote(uuid, servicename);
                Map<TimeUnit, Long> stuff = new HashMap<TimeUnit, Long>();
                if (check != null) {
                    stuff = computeDiff(check, now);

                }
                if (check == null || stuff.get(TimeUnit.DAYS) >= 1) {

                    player.sendMessage(ChatColor.YELLOW + "[" + ChatColor.RED + "✘" + ChatColor.YELLOW + "] " + ChatColor.GOLD + service + ChatColor.WHITE + " ➽ " + ChatColor.GREEN + services.get(service));
                } else {
                    player.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GREEN + "✔" + ChatColor.YELLOW + "] " + ChatColor.GOLD + service + ChatColor.WHITE + " ➽ " + ChatColor.GREEN + services.get(service));

                }


            }

            player.sendMessage("");
            return true;

        }
        return false;
    }

    @EventHandler(ignoreCancelled = false)
    public void onVote(VotifierEvent e) {
        Date date = new Date();
        Timestamp now = new Timestamp(date.getTime());
        Vote vote = e.getVote();
        System.out.println("[OBAMVote] Received: " + vote);
        String user = vote.getUsername();
        String service = vote.getServiceName();

        System.out.println("[OBAMVote] User: " + user);
        System.out.println("[OBAMVote] Service: " + service);
        if (user.equals("Test Notification")) {

            user = "GloriousKoch";
        }
        if (ObamAPI.isOBAMPlayer(user)) {
            Random rand = new Random();
            double tickets = (double) rand.nextInt(225) + 26;
            double stubs = 1;
            Bukkit.broadcastMessage(ChatColor.GOLD + "[OBAMVote] " + ChatColor.YELLOW + user + ChatColor.DARK_AQUA + " has voted using " + ChatColor.YELLOW + service + ChatColor.DARK_AQUA + " and received " + ChatColor.YELLOW + String.valueOf(tickets) + " Tickets" + ChatColor.DARK_AQUA + ", and " + ChatColor.YELLOW + String.valueOf(stubs) + " Stubs" + ChatColor.DARK_AQUA + "!");

            ObamAPI.addStubs(ObamAPI.getUUID(user), stubs, "SYSTEM", "OBAMVote Reward");
            ObamAPI.addTickets(ObamAPI.getUUID(user), tickets, "SYSTEM", "OBAMVote Reward");
            setVote(ObamAPI.getUUID(user), service, now);
            try {
                BungeeMessenger.sendVoteBroadcast(user, service, tickets, stubs);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {

            System.out.println("[OBAMVote] The user: " + user + " is not a registered obam player!");
        }


    }


}
