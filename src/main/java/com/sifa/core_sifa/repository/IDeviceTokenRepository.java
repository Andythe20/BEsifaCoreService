package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IDeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByAppVersionNot(String appVersion);

    List<DeviceToken> findByPlatform(String platform);

    Optional<DeviceToken> findByDeviceId(String deviceId);

    List<DeviceToken> findByEmailUsuario(String emailUsuario);

    long countByStatus(String status);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.status = :status WHERE d.lastHeartbeatAt IS NULL OR d.lastHeartbeatAt < :threshold")
    int markInactiveSince(@Param("status") String status, @Param("threshold") LocalDateTime threshold);
}
