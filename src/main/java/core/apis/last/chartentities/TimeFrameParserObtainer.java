package core.apis.last.chartentities;

import core.exceptions.LastFmException;
import org.json.JSONObject;

import java.util.function.BiFunction;

public interface TimeFrameParserObtainer {
    BiFunction<JSONObject, Integer, UrlCapsule> obtainParse() throws LastFmException;
}
