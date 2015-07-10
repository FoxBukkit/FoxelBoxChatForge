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

import com.foxelbox.chat.html.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.UnmarshallerHandler;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

public class HTMLParser {
    static class WhitespaceAwareUnmarshallerHandler implements ContentHandler {
        private final UnmarshallerHandler uh;
        public WhitespaceAwareUnmarshallerHandler( UnmarshallerHandler uh ) {
            this.uh = uh;
        }
        /**
         * Replace all-whitespace character blocks with the character '\u000B',
         * which satisfies the following properties:
         *
         * 1. "\u000B".matches( "\\s" ) == true
         * 2. when parsing XmlMixed content, JAXB does not suppress the whitespace
         **/
        public void characters(
                char[] ch, int start, int length
        ) throws SAXException {
            for ( int i = start + length - 1; i >= start; --i )
                if ( !Character.isWhitespace( ch[ i ] ) ) {
                    uh.characters( ch, start, length );
                    return;
                }
            Arrays.fill( ch, start, start + length, '\u000B' );
            uh.characters( ch, start, length );
        }
        /* what follows is just blind delegation monkey code */
        public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException { uh.characters( ch, start, length ); }
        public void endDocument() throws SAXException { uh.endDocument(); }
        public void endElement( String uri, String localName, String name ) throws SAXException { uh.endElement( uri,  localName, name ); }
        public void endPrefixMapping( String prefix ) throws SAXException { uh.endPrefixMapping( prefix ); }
        public void processingInstruction( String target, String data ) throws SAXException { uh.processingInstruction(  target, data ); }
        public void setDocumentLocator( Locator locator ) { uh.setDocumentLocator( locator ); }
        public void skippedEntity( String name ) throws SAXException { uh.skippedEntity( name ); }
        public void startDocument() throws SAXException { uh.startDocument(); }
        public void startElement( String uri, String localName, String name, Attributes atts ) throws SAXException { uh.startElement( uri, localName, name, atts ); }
        public void startPrefixMapping( String prefix, String uri ) throws SAXException { uh.startPrefixMapping( prefix, uri ); }
    }

    /*
     * ChatComponentText = TextComponent
     * ChatMessage = TranslatableComponent
     * ChatModifier = Style
     * ChatClickable = ClickEvent
     * ChatHoverable = HoverEvent
     */
    public static String formatParams(String xmlSource, String... params) {
        return String.format(xmlSource, xmlEscapeArray(params));
    }

    private static String[] xmlEscapeArray(String[] in) {
        final String[] out = new String[in.length];
        for(int i = 0; i < in.length; i++)
            out[i] = escape(in[i]);
        return out;
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T unmarshal(JAXBContext ctx, String strData, boolean flgWhitespaceAware) throws Exception {
        UnmarshallerHandler uh = ctx.createUnmarshaller().getUnmarshallerHandler();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        xr.setContentHandler( flgWhitespaceAware ? new WhitespaceAwareUnmarshallerHandler( uh ) : uh );
        xr.parse( new InputSource( new StringReader( strData ) ) );
        return (T)uh.getResult();
    }

    public static ChatComponentStyle parse(String xmlSource) throws Exception {
        xmlSource = "<span>" + xmlSource + "</span>";

        final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        final Element element = unmarshal(jaxbContext, xmlSource, true);

        return element.getDefaultNmsComponent();
    }

    public static ChatComponentStyle format(String format) throws Exception {
        return parse(format);
    }

    public static boolean sendToAll(FoxelBoxChatMod plugin, String format) {
        return sendToPlayers(plugin, plugin.playerHelper.getOnlinePlayers(), format);
    }

    public static boolean sendToPlayers(FoxelBoxChatMod plugin, Collection<EntityPlayerMP> targetPlayers, String format) {
        try {
            final S02PacketChat packet = createChatPacket(format);

            for (EntityPlayerMP commandSender : targetPlayers) {
                plugin.playerHelper.sendPacketToPlayer(commandSender, packet);
            }

            return true;
        }
        catch (Exception e) {
            System.out.println("ERROR ON MESSAGE: " + format);
            e.printStackTrace();
            MinecraftServer.getServer().addChatMessage(new ChatComponentText("Error parsing XML"));

            return false;
        }
    }

    public static boolean sendToPlayer(FoxelBoxChatMod plugin, EntityPlayerMP player, String format) {
        try {
            plugin.playerHelper.sendPacketToPlayer(player, createChatPacket(format));

            return true;
        } catch (Exception e) {
            System.out.println("ERROR ON MESSAGE: " + format);
            e.printStackTrace();
            player.addChatMessage(new ChatComponentText("Error parsing XML"));

            return false;
        }
    }

    public static boolean sendToPlayer(EntityPlayerMP commandSender, String format) {
        return sendToPlayer(commandSender, format);
    }

    private static String parsePlain(String format) {
        return format; // TODO: strip XML tags
    }

    private static S02PacketChat createChatPacket(String format) throws Exception {
        return new S02PacketChat(format(format));
    }

    public static String escape(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&apos;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }
}
