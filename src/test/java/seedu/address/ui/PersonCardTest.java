package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalPersons.ALICE;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;

public class PersonCardTest {

    @BeforeAll
    public static void setUpClass() {
        Platform.startup(() -> { });
    }

    @Test
    public void constructor_validPerson_createsPersonCard() {
        PersonCard personCard = new PersonCard(ALICE, 1);

        assertEquals(ALICE, personCard.person);
    }
}
