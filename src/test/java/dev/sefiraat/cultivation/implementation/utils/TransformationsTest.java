package dev.sefiraat.cultivation.implementation.utils;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransformationsTest {
    @Test
    void itemDisplaysAlwaysReceiveModernRotationWithoutParsingServerVersion() {
        for (Transformations design : Transformations.values()) {
            Transformation original = design.getTransformation(false);
            Transformation display = design.getTransformation(true);
            Quaternionf expected = new Quaternionf(original.getRightRotation()).rotateY((float) Math.PI);
            assertEquals(expected, display.getRightRotation(), design.name());
            assertEquals(original.getTranslation(), display.getTranslation(), design.name());
            assertEquals(original.getScale(), display.getScale(), design.name());
            assertEquals(original.getLeftRotation(), display.getLeftRotation(), design.name());
        }
    }

    @Test
    void repeatedCallsDoNotAccumulateRotationOnTheStoredDesign() {
        for (Transformations design : Transformations.values()) {
            Quaternionf original = new Quaternionf(design.getTransformation(false).getRightRotation());
            Quaternionf first = new Quaternionf(design.getTransformation(true).getRightRotation());
            Quaternionf second = new Quaternionf(design.getTransformation(true).getRightRotation());
            assertEquals(first, second, design.name());
            assertEquals(original, design.getTransformation(false).getRightRotation(), design.name());
        }
    }

    @Test
    void defaultUsesItemDisplayAndNonItemDisplayRetainsOriginalDesign() {
        for (Transformations design : Transformations.values()) {
            assertEquals(design.getTransformation(true), design.getTransformation(), design.name());
            assertSame(design.getTransformation(false), design.getTransformation(false), design.name());
        }
    }
}
