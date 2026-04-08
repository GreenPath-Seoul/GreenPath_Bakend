package com.seoul.greenpath.domain.course.repository;

import com.seoul.greenpath.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCode(String code);
    List<Course> findAllByCodeIn(List<String> codes);
}
