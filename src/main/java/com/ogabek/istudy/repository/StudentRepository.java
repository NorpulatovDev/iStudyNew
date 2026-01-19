package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

        @Query("SELECT s FROM Student s WHERE s.branch.id = :branchId AND s.deleted = false")
        List<Student> findByBranchId(@Param("branchId") Long branchId);

        @Query("SELECT s FROM Student s WHERE s.branch.id = :branchId AND s.deleted = false AND " +
                        "(LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
        List<Student> findByBranchIdAndFullName(@Param("branchId") Long branchId, @Param("name") String name);

        @Query("SELECT s FROM Student s WHERE s.branch.id = :branchId AND s.deleted = false AND s.id NOT IN " +
                        "(SELECT DISTINCT p.student.id FROM Payment p WHERE p.paymentYear = :year AND p.paymentMonth = :month)")
        List<Student> findUnpaidStudentsByBranchAndMonth(@Param("branchId") Long branchId,
                        @Param("year") int year, @Param("month") int month);

        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.student.id = :studentId " +
                        "AND p.paymentYear = :year AND p.paymentMonth = :month")
        BigDecimal getTotalPaidByStudentInMonth(@Param("studentId") Long studentId,
                        @Param("year") int year, @Param("month") int month);

        @Query("SELECT MAX(p.createdAt) FROM Payment p WHERE p.student.id = :studentId")
        LocalDateTime getLastPaymentDate(@Param("studentId") Long studentId);

        @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.student.id = :studentId " +
                        "AND p.paymentYear = :year AND p.paymentMonth = :month")
        Boolean hasStudentPaidInMonth(@Param("studentId") Long studentId,
                        @Param("year") int year, @Param("month") int month);

        @Query("SELECT COALESCE(SUM(g.price), 0) FROM Group g " +
                        "JOIN g.students s WHERE s.id = :studentId AND g.deleted = false")
        BigDecimal getExpectedMonthlyPaymentForStudent(@Param("studentId") Long studentId);

        @Query("SELECT s FROM Student s LEFT JOIN FETCH s.branch WHERE s.branch.id = :branchId AND s.deleted = false ORDER BY s.lastName ASC, s.firstName ASC")
        List<Student> findByBranchIdWithBranch(@Param("branchId") Long branchId);

        boolean existsByBranchIdAndFirstNameAndLastNameAndPhoneNumberAndDeletedFalse(Long branchId, String firstName,
                        String lastName, String phoneNumber);
}