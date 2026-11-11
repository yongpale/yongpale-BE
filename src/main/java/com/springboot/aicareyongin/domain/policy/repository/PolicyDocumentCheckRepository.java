package com.springboot.aicareyongin.domain.policy.repository;

import com.springboot.aicareyongin.domain.policy.entity.PolicyDocumentCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyDocumentCheckRepository extends JpaRepository<PolicyDocumentCheck, Long> {

    @Query("select c.documentCode from PolicyDocumentCheck c where c.user.id = :userId and c.policy.id = :policyId")
    List<String> findDocumentCodes(@Param("userId") Long userId, @Param("policyId") Long policyId);

    // 체크 버튼을 빠르게 두 번 눌러도 중복 행이 생기지 않도록 이미 있으면 무시한다.
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO policy_document_checks (user_id, policy_id, document_code, created_at)
            VALUES (:userId, :policyId, :documentCode, UTC_TIMESTAMP(6))
            """, nativeQuery = true)
    int insertIfMissing(@Param("userId") Long userId, @Param("policyId") Long policyId,
                        @Param("documentCode") String documentCode);

    @Modifying
    @Query("delete from PolicyDocumentCheck c "
            + "where c.user.id = :userId and c.policy.id = :policyId and c.documentCode = :documentCode")
    int deleteCheck(@Param("userId") Long userId, @Param("policyId") Long policyId,
                    @Param("documentCode") String documentCode);
}
