package core.commands;

import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.exceptions.LastFmException;
import core.parsers.ArtistAlbumParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ArtistAlbumParameters;
import dao.ChuuService;
import dao.entities.*;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

public class TasteAlbumCommand extends BaseTasteCommand<ArtistAlbumParameters> {
    private final DiscogsApi discogsApi;
    private final Spotify spotifyApi;

    public TasteAlbumCommand(ChuuService dao) {
        super(dao);
        this.discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        this.spotifyApi = SpotifySingleton.getInstance();
        this.thumbnailPerRow = false;
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public String getEntity(ArtistAlbumParameters params) {
        return params.getAlbum() + " tracks";
    }

    @Override
    public @Nullable String hasCustomUrl(ArtistAlbumParameters params) {
        return params.getScrobbledArtist().getUrl() != null && !params.getScrobbledArtist().getUrl().isBlank() ? params.getScrobbledArtist().getUrl() : null;
    }

    @Override
    public Parser<ArtistAlbumParameters> initParser() {
        return new ArtistAlbumParser(getService(), lastFM, new OptionalEntity("list", "display in a list format"));
    }

    @Override
    public String getDescription() {
        return "Compare your top tracks of an album with another user";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tastealbum", "tastealb", "tasteal", "compal", "compareal");
    }


    @Override
    public Pair<LastFMData, LastFMData> getUserDatas(MessageReceivedEvent e, ArtistAlbumParameters params) throws InstanceNotFoundException {
        User author = params.getE().getAuthor();
        LastFMData secondUser = params.getLastFMData();
        if (author.getIdLong() == secondUser.getDiscordId()) {
            sendMessageQueue(e, "You need to provide at least one other user (ping,discord id,tag format, u:username or lfm:lastfm_name )");

            return null;
        }
        LastFMData ogData = getService().findLastFMData(author.getIdLong());
        return Pair.of(ogData, secondUser);
    }

    @Override
    public ResultWrapper<UserArtistComparison> getResult(LastFMData og, LastFMData second, ArtistAlbumParameters params) throws LastFmException {
        boolean isList = params.hasOptional("list");
        String artist = params.getArtist();
        ScrobbledArtist scrobbledArtist = new ScrobbledArtist(artist, 0, null);
        params.setScrobbledArtist(scrobbledArtist);
        CommandUtil.validate(getService(), scrobbledArtist, lastFM, discogsApi, spotifyApi);
        long albumvalidate = CommandUtil.albumvalidate(getService(), scrobbledArtist, lastFM, params.getAlbum());
        return getService().getSimilaritiesAlbumTracks(List.of(og.getName(), second.getName()), albumvalidate, isList ? 200 : Integer.MAX_VALUE);
    }

    @Override
    public Pair<Integer, Integer> getTasteBar(ResultWrapper<UserArtistComparison> resultWrapper, UserInfo og, UserInfo second, ArtistAlbumParameters params) {
        return Pair.of(resultWrapper.getResultList().stream().mapToInt(UserArtistComparison::getCountA).sum(), resultWrapper.getResultList().stream().mapToInt(UserArtistComparison::getCountB).sum());

    }


    @Override
    public String getName() {
        return "Taste Album";
    }
}
