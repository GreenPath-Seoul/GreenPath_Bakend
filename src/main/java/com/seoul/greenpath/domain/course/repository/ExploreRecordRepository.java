package com.seoul.greenpath.domain.course.repository;

import com.seoul.greenpath.domain.course.entity.ExploreRecord;
import com.seoul.greenpath.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExploreRecordRepository extends JpaRepository<ExploreRecord, Long> {
    List<ExploreRecord> findTop5ByMemberOrderByCreatedAtDesc(Member member);
}
