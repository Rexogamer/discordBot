package main.commands;

import dao.DaoImplementation;
import dao.entities.SecondsTimeFrameCount;
import dao.entities.TimeFrameEnum;
import dao.entities.TimestampWrapper;
import dao.entities.Track;
import main.exceptions.LastFMNoPlaysException;
import main.exceptions.LastFmEntityNotFoundException;
import main.exceptions.LastFmException;
import main.parsers.OnlyUsernameParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import javax.management.InstanceNotFoundException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyCommand extends ConcurrentCommand {

	public WeeklyCommand(DaoImplementation dao) {
		super(dao);
		parser = new OnlyUsernameParser(dao);
	}

	@Override
	String getDescription() {
		return "Weekly Description";
	}

	@Override
	List<String> getAliases() {
		return Arrays.asList("week", "weekly");
	}

	@Override
	void onCommand(MessageReceivedEvent e) {
		String[] parse = parser.parse(e);
		if (parse == null)
			return;
		String lastfmName = parse[0];
		try {
			Map<Track, Integer> durationsFromWeek = lastFM.getDurationsFromWeek(lastfmName);

			Instant instant = Instant.now();
			ZoneId zoneId = ZoneOffset.UTC;
			ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
			ZonedDateTime zdtStart = zdt.toLocalDate().atStartOfDay(zoneId);
			ZonedDateTime zdtPrevious7Days = zdtStart.plusDays(-7);
			Instant from = Instant.from(zdtPrevious7Days);
			Instant to = Instant.from(zdtStart);

			MultiMap<LocalDateTime, Map.Entry<Integer, Integer>> map = new MultiValueMap<>();

			List<TimestampWrapper<Track>> tracksAndTimestamps = lastFM
					.getTracksAndTimestamps(lastfmName, (int) from.getEpochSecond(), (int) to.getEpochSecond());
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, d/M:", Locale.UK);
			SecondsTimeFrameCount minutesWastedOnMusicDaily = new SecondsTimeFrameCount(TimeFrameEnum.WEEK);

			for (TimestampWrapper<Track> tracksAndTimestamp : tracksAndTimestamps) {
				Integer seconds;
				if ((seconds = durationsFromWeek.get(tracksAndTimestamp.getWrapped())) == null) {
					seconds = 200;
				}
				Instant trackInstant = Instant.ofEpochSecond(tracksAndTimestamp.getTimestamp());
				ZonedDateTime tempZdt = ZonedDateTime.ofInstant(trackInstant, zoneId);
				tempZdt = tempZdt.toLocalDate().atStartOfDay(zoneId);
				map.put(tempZdt.toLocalDateTime(), Map.entry(tracksAndTimestamp.getTimestamp(), seconds));
			}
			StringBuilder s = new StringBuilder();
			map.entrySet().stream().sorted((x, y) -> {
						LocalDateTime key = x.getKey();
						LocalDateTime key1 = y.getKey();
						return key.compareTo(key1);
					}
			).forEach(x -> {
				LocalDateTime time = x.getKey();
				List<Map.Entry<Integer, Integer>> value = (List<Map.Entry<Integer, Integer>>) x.getValue();
				int seconds = value.stream().mapToInt(Map.Entry::getValue).sum();

				minutesWastedOnMusicDaily.setSeconds(seconds);
				minutesWastedOnMusicDaily.setCount(value.size());

				s.append("**")
						.append(time.format(dtf))
						.append("** ")
						.append(minutesWastedOnMusicDaily.getMinutes()).append(" minutes, ")
						.append(String
								.format("(%d:%02dh)", minutesWastedOnMusicDaily.getHours(),
										minutesWastedOnMusicDaily.getRemainingMinutes()))

						.append(" on ").append(minutesWastedOnMusicDaily.getCount())
						.append(" tracks\n");
			});
			long userId = getDao().getDiscordIdFromLastfm(lastfmName, e.getGuild().getIdLong());

			Member whoD = e.getGuild().getMemberById(userId);
			String name = whoD == null ? lastfmName : whoD.getEffectiveName();

			MessageBuilder mes = new MessageBuilder();
			EmbedBuilder embedBuilder = new EmbedBuilder();
			embedBuilder.setDescription(s);
			embedBuilder.setColor(CommandUtil.randomColor());
			embedBuilder.setTitle(name + "'s week", CommandUtil.getLastFmUser(lastfmName));
			if (whoD != null)
				embedBuilder.setThumbnail(whoD.getUser().getAvatarUrl());

			e.getChannel().sendMessage(mes.setEmbed(embedBuilder.build()).build()).queue();
		} catch (LastFMNoPlaysException ex) {
			parser.sendError(parser.getErrorMessage(3), e);
		} catch (LastFmEntityNotFoundException ex) {
			parser.sendError(parser.getErrorMessage(4), e);
		} catch (LastFmException ex) {
			parser.sendError("Internal Service Error, try again later", e);
		} catch (InstanceNotFoundException ex) {
			parser.sendError(parser.getErrorMessage(1), e);


		}
	}


	@Override
	String getName() {
		return "Weekly  ";
	}
}