package com.frotty27.rpgmobs.logs;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Arrays;

public final class RPGMobsLogger {

    private RPGMobsLogger() {
    }

    private static volatile RPGMobsConfig config;

    public static void init(RPGMobsConfig RPGMobsConfig) {
        config = RPGMobsConfig;
    }

    public static void debug(HytaleLogger logger, String format, RPGMobsLogLevel level, Object... args) {
        RPGMobsConfig currentConfig = config;
        if (currentConfig == null || !currentConfig.debugConfig.isDebugModeEnabled) return;

        log(logger, format, level, args);
    }

    public static void log(HytaleLogger logger, String format, RPGMobsLogLevel level, Object... args) {
        String formattedMessage = formatSafe(format, args);

        switch (level) {
            case INFO -> logger.atInfo().log("%s", formattedMessage);
            case WARNING -> logger.atWarning().log("%s", formattedMessage);
            case SEVERE -> logger.atSevere().log("%s", formattedMessage);
        }
    }

    private static String formatSafe(String format, Object... args) {
        if (args == null || args.length == 0) return format;

        Object[] safeArguments = new Object[args.length];
        for (int argumentIndex = 0; argumentIndex < args.length; argumentIndex++) {
            Object argument = args[argumentIndex];

            if (argument == null) {
                safeArguments[argumentIndex] = "null";
                continue;
            }

            if (argument.getClass().isArray()) {
                safeArguments[argumentIndex] = deepArrayToString(argument);
                continue;
            }

            safeArguments[argumentIndex] = argument;
        }

        try {
            return String.format(format, safeArguments);
        } catch (Throwable formattingError) {
            return format + " | args=" + Arrays.toString(safeArguments);
        }
    }

    private static String deepArrayToString(Object array) {
        if (array instanceof Object[] objectArray) return Arrays.deepToString(objectArray);
        if (array instanceof int[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof long[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof float[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof double[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof boolean[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof byte[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof short[] primitiveArray) return Arrays.toString(primitiveArray);
        if (array instanceof char[] primitiveArray) return Arrays.toString(primitiveArray);

        return String.valueOf(array);
    }
}
