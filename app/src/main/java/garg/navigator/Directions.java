package garg.navigator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amish Naidu on 08-10-2018.
 */

public enum Directions {
    Straight("straight"),
    SlightLeft("slight-left"),
    SlightRight("slight-right"),
    Left("left"),
    Right("right"),
    UTurnLeft("uturn-left"),
    UTurnRight("uturn-right"),
    ;


    private final String directionString;

    Directions(String str) {
        directionString = str;
    }

    @Override
    public String toString() {
        return directionString;
    }

    private static Map<String, Directions> dirMap;
    static {
        dirMap = new HashMap<>();
        dirMap.put("turn-slight-left", SlightLeft);
        dirMap.put("ramp-left", SlightLeft);
        dirMap.put("fork-left", SlightLeft);

        dirMap.put("turn-slight-right", SlightRight);
        dirMap.put("ramp-right", SlightRight);
        dirMap.put("fork-right", SlightRight);

        dirMap.put("turn-sharp-right", Right);
        dirMap.put("turn-right", Right);
        dirMap.put("roundabout-right", Right);

        dirMap.put("turn-sharp-left", Left);
        dirMap.put("turn-left", Left);
        dirMap.put("roundabout-left", Left);

        dirMap.put("uturn-left", UTurnLeft);

        dirMap.put("uturn-right", UTurnRight);
    }

    static Directions fromString(String str) {
        str = str.toLowerCase();
        if (dirMap.containsKey(str))
            return dirMap.get(str);
        else
            return Straight;
    }
}
