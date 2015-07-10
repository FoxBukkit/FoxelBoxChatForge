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
package com.foxelbox.chat.html;

import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class Tr extends Element {
    @XmlAttribute(required = true)
    private String key;

    @Override
    protected void modifyStyle(ChatStyle style) {
        // TODO: get rid of this
    }

    @Override
    public List<ChatComponentStyle> getNmsComponents(ChatStyle style, boolean condenseElements) throws Exception {
        final List<ChatComponentStyle> components = super.getNmsComponents(style, true);

        final ChatComponentStyle translateComponent = new ChatComponentTranslation(key, components.toArray());
        translateComponent.setChatStyle(style);
        return Arrays.asList(translateComponent);
    }
}
