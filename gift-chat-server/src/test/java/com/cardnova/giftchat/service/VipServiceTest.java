package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.TradeOrderRepository;
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
    private TradeOrderRepository tradeOrderRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void derivesVipLevelsFromCompletedLifetimeUsdVolume() {
        VipService service = new VipService(vipPointLedgerRepository, tradeOrderRepository, currentUserService);

        assertLevel(service, "new", false, "0", "VIP0");
        assertLevel(service, "first", true, "0", "VIP1");
        assertLevel(service, "v2", true, "1000", "VIP2");
        assertLevel(service, "v3", true, "5000", "VIP3");
        assertLevel(service, "v4", true, "10000", "VIP4");
        assertLevel(service, "v5", true, "50000", "VIP5");
    }

    private void assertLevel(VipService service, String userId, boolean completed, String volume, String expected) {
        when(tradeOrderRepository.existsByOwnerUser_IdAndStatusCodeIgnoreCase(userId, "COMPLETED")).thenReturn(completed);
        when(tradeOrderRepository.sumCompletedBaseAmountUsdByOwnerUserId(userId)).thenReturn(new BigDecimal(volume));
        VipSummary summary = service.summaryForUser(userId);
        assertEquals(expected, summary.level());
    }
}
