package com.seoul.greenpath.domain.course.repository;

import com.seoul.greenpath.domain.course.entity.CourseStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CourseStopRepository extends JpaRepository<CourseStop, Long> {
    Optional<CourseStop> findByCourseIdAndStopOrder(Long courseId, Integer stopOrder);
    Optional<CourseStop> findByCode(String code);
    boolean existsByCourseCodeAndStopOrder(String courseCode, Integer stopOrder);
}
