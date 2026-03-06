package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public sealed interface AbilityConfigField {

    String label();

    boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig);

    void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination);

    record PerTierFloat(String label,
                        Function<RPGMobsConfig.AbilityConfig, float[]> getter,
                        BiConsumer<RPGMobsConfig.AbilityConfig, float[]> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !Arrays.equals(getter.apply(editConfig), getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            float[] array = getter.apply(source);
            setter.accept(destination, Arrays.copyOf(array, array.length));
        }
    }

    record PerTierInt(String label,
                      Function<RPGMobsConfig.AbilityConfig, int[]> getter,
                      BiConsumer<RPGMobsConfig.AbilityConfig, int[]> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !Arrays.equals(getter.apply(editConfig), getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            int[] array = getter.apply(source);
            setter.accept(destination, Arrays.copyOf(array, array.length));
        }
    }

    record ScalarFloat(String label,
                       Function<RPGMobsConfig.AbilityConfig, Float> getter,
                       BiConsumer<RPGMobsConfig.AbilityConfig, Float> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !getter.apply(editConfig).equals(getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            setter.accept(destination, getter.apply(source));
        }
    }

    record ScalarInt(String label,
                     Function<RPGMobsConfig.AbilityConfig, Integer> getter,
                     BiConsumer<RPGMobsConfig.AbilityConfig, Integer> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !getter.apply(editConfig).equals(getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            setter.accept(destination, getter.apply(source));
        }
    }

    record ScalarDouble(String label,
                        Function<RPGMobsConfig.AbilityConfig, Double> getter,
                        BiConsumer<RPGMobsConfig.AbilityConfig, Double> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !getter.apply(editConfig).equals(getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            setter.accept(destination, getter.apply(source));
        }
    }

    record ScalarBoolean(String label,
                         Function<RPGMobsConfig.AbilityConfig, Boolean> getter,
                         BiConsumer<RPGMobsConfig.AbilityConfig, Boolean> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !getter.apply(editConfig).equals(getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            setter.accept(destination, getter.apply(source));
        }
    }

    record ScalarString(String label,
                        Function<RPGMobsConfig.AbilityConfig, String> getter,
                        BiConsumer<RPGMobsConfig.AbilityConfig, String> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !Objects.equals(getter.apply(editConfig), getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            setter.accept(destination, getter.apply(source));
        }
    }

    record StringList(String label,
                      Function<RPGMobsConfig.AbilityConfig, List<String>> getter,
                      BiConsumer<RPGMobsConfig.AbilityConfig, List<String>> setter)
            implements AbilityConfigField {
        @Override
        public boolean hasChanged(RPGMobsConfig.AbilityConfig editConfig, RPGMobsConfig.AbilityConfig savedConfig) {
            return !Objects.equals(getter.apply(editConfig), getter.apply(savedConfig));
        }
        @Override
        public void deepCopy(RPGMobsConfig.AbilityConfig source, RPGMobsConfig.AbilityConfig destination) {
            var list = getter.apply(source);
            setter.accept(destination, list != null ? new ArrayList<>(list) : new ArrayList<>());
        }
    }
}
