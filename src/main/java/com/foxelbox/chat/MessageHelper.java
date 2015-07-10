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

import static com.foxelbox.chat.HTMLParser.escape;

public class MessageHelper {
	private static final String PLAYER_FORMAT = "<span onClick=\"suggest_command('/pm %1$s ')\"%3$s>%2$s</span>";

	private static final String FB_DEFAULT_COLOR = "dark_purple";
	private static final String FB_ERROR_COLOR = "dark_red";

	public static final String ONLINE_COLOR = "dark_green";
	public static final String OFFLINE_COLOR = "dark_red";

	public static String button(String command, String label, String color, boolean run) {
		return button(command, label, color, run, true);
	}

	public static String button(String command, String label, String color, boolean run, boolean addHover) {
		final String eventType = run ? "run_command" : "suggest_command";
		if(addHover) {
			return String.format("<color name=\"%3$s\" onClick=\"%4$s('%1$s')\" onHover=\"show_text('%1$s')\">[%2$s]</color>", escape(command), escape(label), escape(color), eventType);
		} else {
			return String.format("<color name=\"%3$s\" onClick=\"%4$s('%1$s')\">[%2$s]</color>", escape(command), escape(label), escape(color), eventType);
		}
	}
}
