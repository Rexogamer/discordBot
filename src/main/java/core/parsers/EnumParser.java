package core.parsers;

import core.exceptions.LastFmException;
import core.parsers.params.EnumParameters;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnumParser<T extends Enum<T>> extends Parser<EnumParameters<T>> {
    protected final Class<T> clazz;

    public EnumParser(Class<T> tClass) {
        this.clazz = tClass;
    }

    @Override
    protected void setUpErrorMessages() {

    }

    @Override
    protected EnumParameters<T> parseLogic(MessageReceivedEvent e, String[] words) throws InstanceNotFoundException, LastFmException {
        EnumSet<T> ts = EnumSet.allOf(clazz);
        List<String> collect = ts.stream().map(x -> x.name().replaceAll("_", "-").toLowerCase()).collect(Collectors.toList());
        if (words.length != 1) {
            sendError("Pls introduce only one of the following: **" + String.join("**, **", collect) + "**", e);
            return null;
        }

        Optional<String> first = collect.stream().filter(x -> words[0].equals(x)).findFirst();
        if (first.isEmpty()) {
            sendError("Pls introduce one of the following: " + String.join(",", collect), e);
            return null;
        }
        return new EnumParameters<>(e, Enum.valueOf(clazz, first.get().toUpperCase().replaceAll("-", "_")));


    }

    @Override
    public String getUsageLogic(String commandName) {
        List<String> collect = EnumSet.allOf(clazz).stream().map(x -> x.name().replaceAll("_", "-").toLowerCase()).collect(Collectors.toList());

        return "**" + commandName + " *config_value*** \n" +
                "\tConfig value being one of: **" + String.join("**, **", collect) + "**";
    }
}

