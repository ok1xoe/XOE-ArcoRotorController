package cz.ok1xoe.arcorotor.desktop;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaidenheadLocatorTests {

    @Test
    void parsesFourCharacterLocatorToSquareCenter() {
        ArcoRotorDesktopApplication.LocatorCoordinates coordinates =
                ArcoRotorDesktopApplication.parseMaidenheadLocator("JO70");

        assertThat(coordinates.latitude()).isCloseTo(50.5, withinOneMicrodegree());
        assertThat(coordinates.longitude()).isCloseTo(15.0, withinOneMicrodegree());
    }

    @Test
    void parsesSixCharacterLocatorToSubsquareCenter() {
        ArcoRotorDesktopApplication.LocatorCoordinates coordinates =
                ArcoRotorDesktopApplication.parseMaidenheadLocator("JO70FD");

        assertThat(coordinates.latitude()).isCloseTo(50.1458333333, withinOneMicrodegree());
        assertThat(coordinates.longitude()).isCloseTo(14.4583333333, withinOneMicrodegree());
    }

    @Test
    void trimsAndUppercasesLocatorBeforeParsing() {
        ArcoRotorDesktopApplication.LocatorCoordinates coordinates =
                ArcoRotorDesktopApplication.parseMaidenheadLocator(" jo70fd ");

        assertThat(coordinates.latitude()).isCloseTo(50.1458333333, withinOneMicrodegree());
        assertThat(coordinates.longitude()).isCloseTo(14.4583333333, withinOneMicrodegree());
    }

    @Test
    void parsesEightCharacterLocatorToExtendedSquareCenter() {
        ArcoRotorDesktopApplication.LocatorCoordinates coordinates =
                ArcoRotorDesktopApplication.parseMaidenheadLocator("JO70FD12");

        assertThat(coordinates.latitude()).isCloseTo(50.1354166667, withinOneMicrodegree());
        assertThat(coordinates.longitude()).isCloseTo(14.4291666667, withinOneMicrodegree());
    }

    @Test
    void rejectsInvalidLocator() {
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator("JO7"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator("ZZ99"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator("JOAA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator("JO70F1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArcoRotorDesktopApplication.parseMaidenheadLocator("JO70F!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculatesSolarDaylightSide() {
        ArcoRotorDesktopApplication.SolarPosition solarPosition =
                ArcoRotorDesktopApplication.SolarPosition.at(Instant.parse("2024-03-20T12:06:00Z"));

        assertThat(solarPosition.daylight(0.0, 0.0)).isGreaterThan(0.95);
        assertThat(solarPosition.daylight(0.0, 180.0)).isLessThan(-0.95);
    }

    private static org.assertj.core.data.Offset<Double> withinOneMicrodegree() {
        return org.assertj.core.data.Offset.offset(0.000001);
    }
}
