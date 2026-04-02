package com.seoul.greenpath.domain.course.repository;

import com.seoul.greenpath.domain.course.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByName(String name);
}
