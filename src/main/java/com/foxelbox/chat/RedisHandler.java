/**
 * This file is part of FoxBukkitChat.
 *
 * FoxBukkitChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitChat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitChat.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.chat;

import com.foxelbox.chat.json.ChatMessageIn;
import com.foxelbox.chat.json.ChatMessageOut;
import com.foxelbox.dependencies.redis.AbstractRedisHandler;
import com.google.gson.Gson;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.*;

public class RedisHandler extends AbstractRedisHandler {
    private final FoxelBoxChatMod plugin;
    public RedisHandler(FoxelBoxChatMod plugin) {
        super(plugin.redisManager, "foxbukkit:to_server");
        this.plugin = plugin;
    }

    public void sendMessage(final EntityPlayerMP player, final String message) {
        sendMessage(player, message, "text");
    }

    public void sendMessage(final ChatMessageIn messageIn) {
        if(messageIn == null)
            throw new NullPointerException();
        final String messageJSON;
        synchronized (gson) {
            messageJSON = gson.toJson(messageIn);
        }
        plugin.redisManager.lpush("foxbukkit:from_server", messageJSON);
    }

    public void sendMessage(final EntityPlayerMP player, final String message, final String type) {
        if(player == null || message == null)
            throw new NullPointerException();
        ChatMessageIn messageIn = new ChatMessageIn(plugin, player);
        messageIn.contents = message;
        if(type != null) {
            messageIn.type = type;
        }
        sendMessage(messageIn);
    }

    private static final Gson gson = new Gson();

    @Override
    public void onMessage(final String c_message) {
        try {
            final ChatMessageOut chatMessageOut;
            synchronized (gson) {
                chatMessageOut = gson.fromJson(c_message, ChatMessageOut.class);
            }

            onMessage(chatMessageOut);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMessage(final ChatMessageOut chatMessageOut) {
        try {
            Collection<? extends EntityPlayerMP> allPlayers = plugin.playerHelper.getOnlinePlayers();
            final List<EntityPlayerMP> targetPlayers = new ArrayList<>();
            switch(chatMessageOut.to.type) {
                case "all":
                    targetPlayers.addAll(allPlayers);
                    break;
                case "permission":
                    /*for(String permission : chatMessageOut.to.filter)
                        for (EntityPlayerMP player : allPlayers)
                            if (player.hasPermission(permission) && !targetPlayers.contains(player))
                                targetPlayers.add(player);*/
                    break;
                case "player":
                    for(String playerUUID : chatMessageOut.to.filter)
                        for (EntityPlayerMP player : allPlayers)
                            if (EntityPlayerMP.getUUID(player.getGameProfile()).equals(UUID.fromString(playerUUID)) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
            }

            if(chatMessageOut.type.equals("kick")) {
                plugin.runNextTick(new Runnable() {
                    @Override
                    public void run() {
                        for (EntityPlayerMP target : targetPlayers) {
                            plugin.playerHelper.refreshPlayerListRedis(target);
                            plugin.registeredPlayers.remove(EntityPlayerMP.getUUID(target.getGameProfile()));
                            target.playerNetServerHandler.kickPlayerFromServer(chatMessageOut.contents);
                        }
                    }
                });
                return;
            } else if(chatMessageOut.type.equals("inject")) {
                final String[] contents = chatMessageOut.contents.split("\n");
                plugin.runNextTick(new Runnable() {
                    @Override
                    public void run() {
                        for (EntityPlayerMP target : targetPlayers) {
                            for (String cmd : contents) {
                                target.playerNetServerHandler.processChatMessage(new C01PacketChatMessage(cmd));
                            }
                        }
                    }
                });
                return;
            } else if(!chatMessageOut.type.equals("text")) {
                return;
            }

            if(chatMessageOut.from != null && chatMessageOut.from.uuid != null) {
                Set<UUID> ignoringSet = plugin.playerHelper.getIgnoredBy(chatMessageOut.from.uuid);
                if(ignoringSet != null) {
                    Iterator<EntityPlayerMP> playerIterator = targetPlayers.iterator();
                    while(playerIterator.hasNext()) {
                        if(ignoringSet.contains(EntityPlayerMP.getUUID(playerIterator.next().getGameProfile()))) {
                            playerIterator.remove();
                        }
                    }
                }
            }

            if(targetPlayers.isEmpty()) {
                return;
            }

            if (chatMessageOut.server != null && !chatMessageOut.server.equals(plugin.configuration.getValue("server-name", "Main"))) {
                if(chatMessageOut.contents != null) {
                    chatMessageOut.contents = "<color name=\"dark_green\">[" + chatMessageOut.server + "]</color> " + chatMessageOut.contents;
                }
            }

            if(chatMessageOut.contents != null) {
                HTMLParser.sendToPlayers(plugin, targetPlayers, chatMessageOut.contents);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
