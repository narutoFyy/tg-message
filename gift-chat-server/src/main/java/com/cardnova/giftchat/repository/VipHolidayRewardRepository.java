package com.cardnova.giftchat.repository;
import com.cardnova.giftchat.entity.VipHolidayRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface VipHolidayRewardRepository extends JpaRepository<VipHolidayRewardEntity, String> {
    List<VipHolidayRewardEntity> findAllByOrderByHolidayDateDesc();
    List<VipHolidayRewardEntity> findByCountryCodeAndHolidayDateAndEnabledTrueOrderByHolidayNameAsc(String countryCode, LocalDate holidayDate);
    Optional<VipHolidayRewardEntity> findByCountryCodeAndHolidayCodeAndHolidayDate(String countryCode, String holidayCode, LocalDate holidayDate);
}
