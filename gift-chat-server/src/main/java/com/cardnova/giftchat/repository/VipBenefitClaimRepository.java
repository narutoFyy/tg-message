package com.cardnova.giftchat.repository;
import com.cardnova.giftchat.entity.VipBenefitClaimEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface VipBenefitClaimRepository extends JpaRepository<VipBenefitClaimEntity, String> {
    boolean existsBySourceKey(String sourceKey);
    List<VipBenefitClaimEntity> findByUser_IdOrderByRequestedAtDesc(String userId);
    List<VipBenefitClaimEntity> findAllByOrderByRequestedAtDesc();
    List<VipBenefitClaimEntity> findByUser_IdInAndStatusCode(Collection<String> userIds, String statusCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VipBenefitClaimEntity> findById(String id);
}
