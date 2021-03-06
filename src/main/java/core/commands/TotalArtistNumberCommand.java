package core.commands;

import core.exceptions.LastFmException;
import core.parsers.NumberParser;
import core.parsers.OnlyUsernameParser;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.parsers.params.NumberParameters;
import dao.ChuuService;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.parsers.ExtraParser.LIMIT_ERROR;

public class TotalArtistNumberCommand extends ConcurrentCommand<NumberParameters<ChuuDataParams>> {
    public TotalArtistNumberCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<NumberParameters<ChuuDataParams>> initParser() {


        Map<Integer, String> map = new HashMap<>(2);
        map.put(LIMIT_ERROR, "The number introduced must be positive and not very big");
        String s = "You can also introduce the playcount to only show artists above that number of plays";
        return new NumberParser<>(new OnlyUsernameParser(getService()),
                -0L,
                Integer.MAX_VALUE,
                map, s, false, true, true);

    }

    @Override
    public String getDescription() {
        return ("Number of artists listened by an user");
    }

    @Override
    public List<String> getAliases() {
        return List.of("artists", "art");
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull NumberParameters<ChuuDataParams> params) throws LastFmException, InstanceNotFoundException {

        ChuuDataParams innerParams = params.getInnerParams();
        String lastFmName = innerParams.getLastFMData().getName();
        long discordID = innerParams.getLastFMData().getDiscordId();
        String username = getUserString(e, discordID, lastFmName);
        int threshold = params.getExtraParam().intValue();

        int plays = getService().getUserArtistCount(lastFmName, threshold == 0 ? -1 : threshold);
        String filler = "";
        if (threshold != 0) {
            filler += " with more than " + threshold + " plays";
        }
        sendMessageQueue(e, String.format("**%s** has scrobbled **%d** different artists%s", username, plays, filler));

    }

    @Override
    public String getName() {
        return "Artist count ";
    }
}
