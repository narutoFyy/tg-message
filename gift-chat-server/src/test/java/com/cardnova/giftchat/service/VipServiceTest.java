package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.VipPointLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VipServiceTest {

    @Mock
    private VipPointLedgerRepository vipPointLedgerRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void derivesVipLevelsOnlyFromManualPointLedger() {
        VipService service = new VipService(vipPointLedgerRepository, currentUserService);

        assertLevel(service, "new", "0", "VIP0");
        assertLevel(service, "first", "0.01", "VIP1");
        assertLevel(service, "v2", "1000", "VIP2");
        assertLevel(service, "v3", "5000", "VIP3");
        assertLevel(service, "v4", "10000", "VIP4");
        assertLevel(service, "v5", "50000", "VIP5");
    }

    private void assertLevel(VipService service, String userId, String points, String expected) {
        when(vipPointLedgerRepository.sumPointsByUserId(userId)).thenReturn(new BigDecimal(points));
        VipSummary summary = service.summaryForUser(userId);
        assertEquals(expected, summary.level());
    }
}
