package com.obam.ismooch.obamvote;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.Arrays;

/**
 * Created by troyj_000 on 3/8/2015.
 */
class BungeeMessenger implements PluginMessageListener {

    public static Plugin plugin;

    public BungeeMessenger(Plugin p) {

        p.getServer().getMessenger().registerOutgoingPluginChannel(p, "BungeeCord");
        p.getServer().getMessenger().registerIncomingPluginChannel(p, "BungeeCord", this);
        plugin = p;
    }


    public static void sendVoteBroadcast(String person, String service, Double tickets, Double stubs) throws IOException {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF("VoteBroadcast");


        Player player = Iterables.getFirst(Arrays.asList(Bukkit.getOnlinePlayers()), null);
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        msgout.writeUTF(person);
        msgout.writeUTF(service);
        msgout.writeUTF(String.valueOf(tickets));
        msgout.writeUTF(String.valueOf(stubs));


        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
        if (player != null) {
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }


    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {

        if (!channel.equals("BungeeCord")) {

            return;

        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("VoteBroadcast")) {

            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            String person = null;
            String service = null;
            double stubs = 0;
            double tickets = 0;
            try {
                person = msgin.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                service = msgin.readUTF();
            } catch (IOException e) {

                e.printStackTrace();
            }
            try {
                tickets = Double.valueOf(msgin.readUTF());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                stubs = Double.valueOf(msgin.readUTF());
            } catch (IOException e) {

                e.printStackTrace();
            }


            Bukkit.broadcastMessage(ChatColor.GOLD + "[OBAMVote] " + ChatColor.YELLOW + person + ChatColor.DARK_AQUA + " has voted using " + ChatColor.YELLOW + service + ChatColor.DARK_AQUA + " and received " + ChatColor.YELLOW + String.valueOf(tickets) + " Tickets" + ChatColor.DARK_AQUA + ", and " + ChatColor.YELLOW + String.valueOf(stubs) + " Stubs" + ChatColor.DARK_AQUA + "!");


        }


    }
}
