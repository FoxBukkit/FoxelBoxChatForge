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

import com.foxelbox.dependencies.redis.CacheMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public class PlayerHelper {
    private FoxelBoxChatMod plugin;
    public Map<String,String> playerNameToUUID;
    public Map<String,String> playerUUIDToName;

    public Map<String,String> playerNicks;

    public Map<String,String> ignoredByList;
    private final Map<UUID, Set<UUID>> ignoreCache;

    public void refreshUUID(EntityPlayerMP player) {
        UUID uuid = EntityPlayerMP.getUUID(player.getGameProfile());
        playerUUIDToName.put(uuid.toString(), player.getName());
        playerNameToUUID.put(player.getName().toLowerCase(), uuid.toString());
    }

    public PlayerHelper(FoxelBoxChatMod plugin) {
        this.plugin = plugin;
        playerNameToUUID = plugin.redisManager.createCachedRedisMap("playerNameToUUID");
        playerUUIDToName = plugin.redisManager.createCachedRedisMap("playerUUIDToName");
        playerNicks = plugin.redisManager.createCachedRedisMap("playernicks");
        ignoreCache = new HashMap<>();
        ignoredByList = plugin.redisManager.createCachedRedisMap("ignoredByList").addOnChangeHook(new CacheMap.OnChangeHook() {
            @Override
            public void onEntryChanged(String key, String value) {
                synchronized (ignoreCache) {
                    putIgnoreCache(UUID.fromString(key), value);
                }
            }
        });
    }

    private Set<UUID> putIgnoreCache(UUID uuid, String data) {
        HashSet<UUID> dataSet = new HashSet<>();
        if(data != null && !data.isEmpty()) {
            for (String entry : data.split(",")) {
                dataSet.add(UUID.fromString(entry));
            }
        }
        synchronized (ignoreCache) {
            ignoreCache.put(uuid, dataSet);
        }
        return dataSet;
    }

    public Set<UUID> getIgnoredBy(UUID uuid) {
        synchronized (ignoreCache) {
            Set<UUID> result = ignoreCache.get(uuid);
            if(result == null) {
                result = putIgnoreCache(uuid, ignoredByList.get(uuid.toString()));
            }
            return result;
        }
    }

    public void refreshPlayerListRedis(EntityPlayerMP ignoreMe) {
        Collection<EntityPlayerMP> players = getOnlinePlayers();
        final String keyName = "playersOnline:" + plugin.configuration.getValue("server-name", "Main");
        plugin.redisManager.del(keyName);
        for(EntityPlayerMP ply : players) {
            if(ply.equals(ignoreMe))
                continue;
            plugin.redisManager.sadd(keyName, EntityPlayerMP.getUUID(ply.getGameProfile()).toString());
        }
    }

    public List<EntityPlayerMP> getOnlinePlayers() {
        ArrayList<EntityPlayerMP> ret = new ArrayList<>();
        try {
            Collection players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
            for (Object plyO : players) {
                ret.add((EntityPlayerMP) plyO);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void sendPacketToPlayer(final EntityPlayerMP ply, final Packet packet) {
        ply.playerNetServerHandler.sendPacket(packet);
    }
}
