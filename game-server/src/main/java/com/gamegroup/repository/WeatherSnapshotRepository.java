package com.gamegroup.repository;

import com.gamegroup.entity.WeatherSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeatherSnapshotRepository extends JpaRepository<WeatherSnapshot, Long> {

    Optional<WeatherSnapshot> findByUserIdAndCityAndTargetDate(Long userId, String city, LocalDate targetDate);
}
