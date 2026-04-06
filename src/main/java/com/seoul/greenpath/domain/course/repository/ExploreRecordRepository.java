package com.seoul.greenpath.domain.course.repository;

import com.seoul.greenpath.domain.course.entity.ExploreRecord;
import com.seoul.greenpath.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

public interface ExploreRecordRepository extends JpaRepository<ExploreRecord, Long> {
    List<ExploreRecord> findTop5ByMemberOrderByCreatedAtDesc(Member member);

    @Query("select distinct er.course.id from ExploreRecord er where er.member.id = :memberId")
    Set<Long> findCompletedCourseIdsByMemberId(@Param("memberId") Long memberId);
}
