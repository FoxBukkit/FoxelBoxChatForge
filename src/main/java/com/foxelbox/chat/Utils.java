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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.UUID;

@SuppressWarnings("UnusedDeclaration")
public class Utils {
    public static String concat(Collection<String> parts, int start, String defaultText) {
        // TODO: optimize
        return concatArray(parts.toArray(new String[parts.size()]), start, defaultText);
    }

    public static UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("[CONSOLE]".getBytes());

    public static String XMLEscape(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&apos;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }

    public static String concatArray(String[] array, int start, String defaultText) {
        if (array.length <= start)
            return defaultText;

        if (array.length <= start + 1)
            return array[start]; // optimization

        StringBuilder ret = new StringBuilder(array[start]);
        for(int i = start + 1; i < array.length; i++) {
            ret.append(' ');
            ret.append(array[i]);
        }
        return ret.toString();
    }

    public static <T, E> void setPrivateValueByType(Class<? super T> instanceclass, T instance, Class<?> fieldType, E value) {
        try {
            Field field = null;
            for(Field f : instanceclass.getDeclaredFields()) {
                if(fieldType.isAssignableFrom(f.getType())) {
                    field = f;
                    break;
                }
            }
            if(field == null) {
                throw new Exception("Not found");
            }
            setPrivateValue(instanceclass, instance, field, value);
        } catch (Exception e) {
            System.err.println("Could not set field of type \"" + fieldType + "\" of class \"" + instanceclass.getCanonicalName() + "\" because \"" + e.getMessage() + "\"");
        }
    }

    public static <T, E> void setPrivateValueByName(Class<? super T> instanceclass, T instance, String field, E value) {
        try {
            setPrivateValue(instanceclass, instance, instanceclass.getDeclaredField(field), value);
        } catch (Exception e) {
            System.err.println("Could not set field \"" + field + "\" of class \"" + instanceclass.getCanonicalName() + "\" because \"" + e.getMessage() + "\"");
        }
    }

    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, Field f, E value) {
        try
        {
            Field field_modifiers = Field.class.getDeclaredField("modifiers");
            field_modifiers.setAccessible(true);

            int modifiers = field_modifiers.getInt(f);

            if ((modifiers & Modifier.FINAL) != 0)
                field_modifiers.setInt(f, modifiers & ~Modifier.FINAL);

            f.setAccessible(true);
            f.set(instance, value);
        }
        catch (Exception e) {
            System.err.println("Could not set field \"" + f.getName() + "\" of class \"" + instanceclass.getCanonicalName() + "\" because \"" + e.getMessage() + "\"");
        }
    }
}
