package dev.konqasasas.beat.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerVisibilityPolicyTest {
    @Test void toggleOnlyHidesOtherParticipantsAndNeverAdmins(){
        var policy=new PlayerVisibilityPolicy();UUID viewer=UUID.randomUUID(),participant=UUID.randomUUID(),admin=UUID.randomUUID();
        assertTrue(policy.visible(viewer));assertFalse(policy.shouldHide(viewer,participant,true,false));
        assertFalse(policy.toggle(viewer));
        assertTrue(policy.shouldHide(viewer,participant,true,false));
        assertFalse(policy.shouldHide(viewer,viewer,true,false));
        assertFalse(policy.shouldHide(viewer,admin,true,true));
        assertFalse(policy.shouldHide(viewer,UUID.randomUUID(),false,false));
        assertTrue(policy.toggle(viewer));assertTrue(policy.visible(viewer));
    }

    @Test void preferenceSurvivesReconnectUntilSessionIsCleared(){
        var policy=new PlayerVisibilityPolicy();UUID viewer=UUID.randomUUID();policy.toggle(viewer);
        assertFalse(policy.visible(viewer));policy.clear();assertTrue(policy.visible(viewer));
    }
}
