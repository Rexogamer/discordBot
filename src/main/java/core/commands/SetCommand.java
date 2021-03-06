package core.commands;

import core.Chuu;
import core.exceptions.LastFMNoPlaysException;
import core.exceptions.LastFmEntityNotFoundException;
import core.exceptions.LastFmException;
import core.parsers.ChartableParser;
import core.parsers.Parser;
import core.parsers.SetParser;
import core.parsers.params.WordParameter;
import core.parsers.utils.CustomTimeFrame;
import dao.ChuuService;
import dao.entities.*;
import dao.exceptions.DuplicateInstanceException;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

public class SetCommand extends ConcurrentCommand<WordParameter> {
    public SetCommand(ChuuService dao) {
        super(dao);
        parser = new SetParser();
        this.respondInPrivate = false;

    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.STARTING;
    }

    @Override
    public Parser<WordParameter> initParser() {
        return new SetParser();
    }

    @Override
    public String getDescription() {
        return "Adds you to the system";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("set");
    }

    @Override
    public void onCommand(MessageReceivedEvent e, @NotNull WordParameter params) throws LastFmException, InstanceNotFoundException {


        String lastFmID = params.getWord();
        long guildID = e.getGuild().getIdLong();
        long userId = e.getAuthor().getIdLong();
        //Gets all users in this server

        try {
            lastFM.getUserInfo(List.of(lastFmID));
        } catch (LastFmEntityNotFoundException | IllegalArgumentException ex) {
            sendMessageQueue(e, "The provided username doesn't exist on last.fm, choose another one");
            return;
        }
        List<UsersWrapper> guildlist = getService().getAll(guildID);
        if (guildlist.isEmpty()) {
            getService().createGuild(guildID);
        }

        List<UsersWrapper> list = getService().getAllALL();
        Optional<UsersWrapper> globalName = (list.stream().filter(user -> user.getLastFMName().equals(lastFmID)).findFirst());
        String repeatedMessage = "That username is already registered, if you think this is a mistake, please contact the bot developers";
        if (globalName.isPresent() && (globalName.get().getDiscordID() != userId)) {
            sendMessageQueue(e, repeatedMessage);
            return;
        }

        Optional<UsersWrapper> name = (guildlist.stream().filter(user -> user.getLastFMName().equals(lastFmID)).findFirst());
        //If name is already registered in this server
        if (name.isPresent()) {
            if (name.get().getDiscordID() != userId)
                sendMessageQueue(e, repeatedMessage);
            else
                sendMessageQueue(e, String.format("%s, you are good to go!", CommandUtil.cleanMarkdownCharacter(e.getAuthor().getName())));
            return;
        }

        Optional<UsersWrapper> u = (guildlist.stream().filter(user -> user.getDiscordID() == userId).findFirst());
        //User was already registered in this guild
        if (u.isPresent()) {
            //Registered with different username
            if (!u.get().getLastFMName().equalsIgnoreCase(lastFmID)) {
                sendMessageQueue(e, "Changing your username, might take a while");
                try {
                    getService().changeLastFMName(userId, lastFmID);
                } catch (DuplicateInstanceException ex) {
                    sendMessageQueue(e, repeatedMessage);
                    return;
                }
            } else {
                sendMessageQueue(e, String.format("%s, you are goo d to go!", CommandUtil.cleanMarkdownCharacter(e.getAuthor().getName())));
                return;
            }
            //First Time on the guild
        } else {
            //If it was registered in at least other  guild theres no need to update
            if (getService().getGuildList(userId).stream().anyMatch(guild -> guild != guildID)) {
                //Adds the user to the guild
                if (getService().isUserServerBanned(userId, guildID)) {
                    sendMessageQueue(e, String.format("%s, you have been not allowed to appear on the server leaderboards as a choice of this server admins. Rest of commands should work fine.", CommandUtil.cleanMarkdownCharacter(e.getAuthor().getName())));
                    return;
                }
                getService().addGuildUser(userId, guildID);
                sendMessageQueue(e, String.format("%s, you are good to go!", CommandUtil.cleanMarkdownCharacter(e.getAuthor().getName())));
                return;
            }
        }

        //Never registered before
        MessageBuilder mes = new MessageBuilder();
        mes.setContent("**" + CommandUtil.cleanMarkdownCharacter(e.getAuthor()
                .

                        getName()) + "** has set their last FM name \n Updating your library, wait a moment");
        e.getChannel().sendMessage(mes.build()).
                queue(t -> e.getChannel().
                        sendTyping().
                        queue());
        LastFMData lastFMData = new LastFMData(lastFmID, userId, Role.USER, false, true, WhoKnowsMode.IMAGE, ChartMode.IMAGE, RemainingImagesMode.IMAGE, ChartableParser.DEFAULT_X, ChartableParser.DEFAULT_Y, PrivacyMode.NORMAL, true, false, TimeZone.getDefault());
        lastFMData.setGuildID(guildID);

        getService().insertNewUser(lastFMData);

        try {


            List<ScrobbledArtist> artistData = lastFM.getAllArtists(lastFMData.getName(), new CustomTimeFrame(TimeFrameEnum.ALL));
            getService().insertArtistDataList(artistData, lastFmID);
            sendMessageQueue(e, "Finished updating your artist, now the album/track process will start");
            e.getChannel().sendTyping().queue();
            List<ScrobbledAlbum> albumData = lastFM.getAllAlbums(lastFMData.getName(), new CustomTimeFrame(TimeFrameEnum.ALL));
            getService().albumUpdate(albumData, artistData, lastFmID);
            List<ScrobbledTrack> trackData = lastFM.getAllTracks(lastFMData.getName(), CustomTimeFrame.ofTimeFrameEnum(TimeFrameEnum.ALL));
            getService().trackUpdate(trackData, artistData, lastFmID);
            sendMessageQueue(e, "Successfully updated " + lastFmID + " info!");
            //  e.getGuild().loadMembers((Chuu::caching));
        } catch (
                LastFMNoPlaysException ex) {
            sendMessageQueue(e, "Finished updating " + CommandUtil.cleanMarkdownCharacter(e.getAuthor().getName()) + "'s library, you are good to go!");
        } catch (
                LastFmEntityNotFoundException ex) {
            getService().removeUserCompletely(userId);
            Chuu.getLogger().warn(ex.getMessage(), ex);
            sendMessageQueue(e, "The provided username doesn't exist anymore on last.fm, please re-set your account");
        } catch (
                Throwable ex) {
            System.out.println("Error while updating " + lastFmID + LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_DATE));
            Chuu.getLogger().warn(ex.getMessage(), ex);
            getService().updateUserTimeStamp(lastFmID, 0, null);
            sendMessageQueue(e, String.format("Error downloading %s's library, try to run !update, try again later or contact bot admins if the error persists", CommandUtil.cleanMarkdownCharacter(e.getAuthor()
                    .getName())));
        }

    }

    @Override
    public String getName() {
        return "Set";
    }


}
