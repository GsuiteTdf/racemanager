package com.racemanager.api.importacion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacionCarreraRepository extends JpaRepository<ImportacionCarrera, Long> {
    boolean existsByCarreraIdAndSha256(Long carreraId, String sha256);
    Optional<ImportacionCarrera> findByIdAndCarreraId(Long id, Long carreraId);
}
