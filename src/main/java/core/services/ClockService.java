package core.services;

import core.Chuu;
import core.imagerenderer.CircleRenderer;
import core.imagerenderer.GraphicUtils;
import core.imagerenderer.stealing.jiff.GifSequenceWriter;
import dao.entities.PreBillboardUserDataTimestamped;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClockService {
    public static final Function<TimeZone, Function<PreBillboardUserDataTimestamped, OffsetDateTime>> dateTimeFunctionComposer = (t) -> (x) -> x.getTimestamp().toInstant().atZone(t.toZoneId()).toOffsetDateTime();
    private final ClockMode clockMode;
    private final List<PreBillboardUserDataTimestamped> data;
    private final TimeZone timeZone;
    private final Function<PreBillboardUserDataTimestamped, OffsetDateTime> dateTimeFunction;
    TemporalField weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear();
    public ClockService(ClockMode clockMode, List<PreBillboardUserDataTimestamped> data, TimeZone timeZone) {
        this.clockMode = clockMode;
        this.data = data;
        this.timeZone = timeZone;
        dateTimeFunction = dateTimeFunctionComposer.apply(timeZone);

    }

    public byte[] clockDoer() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(baos)) {

            HashMap<Integer, List<PreBillboardUserDataTimestamped>> collect = data.stream().
                    collect(Collectors.groupingBy(x -> {
                        switch (clockMode) {
                            case BY_WEEK:
                                return byWeek(x);
                            case BY_DAY:
                                return byDay(x);
                            default:
                                throw new IllegalStateException();
                        }
                    }, HashMap::new, Collectors.toList()));
            List<BufferedImage> images = collect.entrySet().parallelStream().sorted(Map.Entry.comparingByKey()).map(
                    t -> {
                        Integer key = t.getKey();
                        List<PreBillboardUserDataTimestamped> value = t.getValue();

                        HashMap<Integer, Long> collect1 = value.stream()
                                .collect(Collectors.groupingBy(x -> dateTimeFunction.apply(x).getHour(),
                                        HashMap::new, Collectors.counting()));
                        byte[] bytes = CircleRenderer.generateImage(clockMode, collect1, key, timeZone);
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                        try {
                            BufferedImage read = ImageIO.read(bais);
                            Graphics2D g = read.createGraphics();
                            GraphicUtils.setQuality(g);
                            g.dispose();
                            return read;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ).collect(Collectors.toList());
            GifSequenceWriter.saveGif(output, images, 0, 300);


        } catch (
                IOException exception) {
            Chuu.getLogger().warn(exception.getMessage(), exception);
        }
        byte[] bytes = baos.toByteArray();
        if (bytes.length == 0)
            return null;
        return bytes;

    }

    private Integer byWeek(PreBillboardUserDataTimestamped x) {
        int i = dateTimeFunction.apply(x).get(weekOfYear);
        int y = dateTimeFunction.apply(x).getYear();
        return y * 1000 + i;
    }

    private Integer byDay(PreBillboardUserDataTimestamped x) {
        return dateTimeFunction.apply(x).getDayOfYear();
    }

    public enum ClockMode {
        BY_WEEK, BY_DAY
    }
}
