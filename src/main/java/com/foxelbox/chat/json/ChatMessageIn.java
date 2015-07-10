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
package com.foxelbox.chat.json;

import com.foxelbox.chat.FoxelBoxChatMod;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class ChatMessageIn {
    public ChatMessageIn(FoxelBoxChatMod plugin, EntityPlayerMP commandSender) {
        this(plugin);
        this.from = new UserInfo(EntityPlayerMP.getUUID(commandSender.getGameProfile()), commandSender.getName());
    }

    public ChatMessageIn(FoxelBoxChatMod plugin) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.context = UUID.randomUUID();
        this.server = plugin.configuration.getValue("server-name", "Main");
    }

    public String server;

    public UserInfo from;

    public long timestamp;
    public UUID context;

    public String type;
    public String contents;
}
