/*
 * Copyright (C) 2012 MineStar.de 
 * 
 * This file is part of VinciCode.
 * 
 * VinciCode is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * VinciCode is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with VinciCode.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.minestar.vincicode.manager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.minestar.minestarlibrary.bookapi.MinestarBook;
import de.minestar.minestarlibrary.messages.Message;
import de.minestar.minestarlibrary.utils.ConsoleUtils;
import de.minestar.minestarlibrary.utils.PlayerUtils;
import de.minestar.vincicode.core.VinciCodeCore;
import de.minestar.vincicode.data.MailBox;

public class MessageManager {

    private Map<String, MailBox> mailBoxMap = new HashMap<String, MailBox>();

    public MessageManager() {
        loadMailBoxes();
    }

    // MAIL BOX AND MESSAGE HANDELING

    private void loadMailBoxes() {
        mailBoxMap = VinciCodeCore.dbHandler.loadMailBoxes();
        ConsoleUtils.printInfo(VinciCodeCore.NAME, "Loaded " + mailBoxMap.size() + " mail boxes!");
    }

    public boolean handleMessage(Message message) {
        String targetName = message.getTarget();
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null && player.isOnline()) {
            PlayerUtils.sendBlankMessage(player, message.getCompleteMessage());
            return true;
        } else
            return handleOfflineMessage(message);

    }

    public boolean handleOfflineMessage(Message message) {
        String targetName = message.getTarget();
        MailBox mailBox = mailBoxMap.get(targetName.toLowerCase());
        // PLAYER HAS NO MAIL BOX
        if (mailBox == null) {
            mailBox = new MailBox();
            mailBoxMap.put(targetName.toLowerCase(), mailBox);
        }
        // SAVE LOCALY AND SAVE IN DATEBASE
        mailBox.add(message);
        return VinciCodeCore.dbHandler.addMessage(message);
    }

    public boolean hasNewMessage(String player) {
        MailBox mailBox = mailBoxMap.get(player.toLowerCase());
        return mailBox != null && mailBox.hasNewMessages();
    }

    public ItemStack getMailBoxItem(String player) {
        MailBox mailBox = mailBoxMap.get(player.toLowerCase());
        MinestarBook myBook;
        if (mailBox == null) {
            myBook = MinestarBook.createWrittenBook(player, "Deine MailBox", Collections.<String> emptyList());
        } else {
            List<String> pages = new ArrayList<String>();
            List<Message> messages = mailBox.getAllMessages();
            for (Message message : messages) {
                pages.add(formatMessage(message));
            }
            myBook = MinestarBook.createWrittenBook(player, "Deine MailBox", pages);
        }

        return myBook.getBukkitItemStack();
    }

    // FORMAT MESSAGES
    private final static DateFormat FORMAT = DateFormat.getDateTimeInstance();

    private String formatMessage(Message message) {
        StringBuilder sBuilder = new StringBuilder(256);
        sBuilder.append("Absender: ");
        sBuilder.append(message.getSender());
        sBuilder.append('\n');
        sBuilder.append("Datum: ");
        sBuilder.append(FORMAT.format(new Date(message.getTimestamp())));
        sBuilder.append('\n');
        sBuilder.append(message.getMessage());

        return sBuilder.toString();
    }

    // CHAT COMMANDS

    private Map<Player, Player> lastSendMap = new HashMap<Player, Player>();

    public void setLastSend(Player sender, Player receiver) {
        lastSendMap.put(sender, receiver);
        lastSendMap.put(receiver, sender);
    }

    public Player getLastReceiver(Player player) {
        return lastSendMap.get(player);
    }

}
