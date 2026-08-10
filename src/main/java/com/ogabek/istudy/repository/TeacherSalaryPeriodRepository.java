package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.TeacherSalaryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherSalaryPeriodRepository extends JpaRepository<TeacherSalaryPeriod, Long> {

    Optional<TeacherSalaryPeriod> findByTeacherIdAndYearAndMonth(Long teacherId, int year, int month);

    boolean existsByTeacherIdAndYearAndMonth(Long teacherId, int year, int month);

    @Query("SELECT DISTINCT p FROM TeacherSalaryPeriod p " +
           "LEFT JOIN FETCH p.lines " +
           "LEFT JOIN FETCH p.teacher " +
           "LEFT JOIN FETCH p.branch " +
           "WHERE p.teacher.id = :teacherId AND p.year = :year AND p.month = :month")
    Optional<TeacherSalaryPeriod> findWithLines(@Param("teacherId") Long teacherId,
                                                @Param("year") int year,
                                                @Param("month") int month);

    @Query("SELECT p FROM TeacherSalaryPeriod p " +
           "LEFT JOIN FETCH p.teacher " +
           "LEFT JOIN FETCH p.branch " +
           "WHERE p.teacher.id = :teacherId " +
           "ORDER BY p.year DESC, p.month DESC")
    List<TeacherSalaryPeriod> findByTeacherIdOrderByPeriodDesc(@Param("teacherId") Long teacherId);

    @Query("SELECT p FROM TeacherSalaryPeriod p " +
           "LEFT JOIN FETCH p.teacher " +
           "LEFT JOIN FETCH p.branch " +
           "WHERE p.branch.id = :branchId AND p.year = :year AND p.month = :month")
    List<TeacherSalaryPeriod> findByBranchAndPeriod(@Param("branchId") Long branchId,
                                                    @Param("year") int year,
                                                    @Param("month") int month);

    @Query("SELECT DISTINCT p.year, p.month FROM TeacherSalaryPeriod p " +
           "WHERE p.teacher.id = :teacherId")
    List<Object[]> findDistinctYearMonthByTeacherId(@Param("teacherId") Long teacherId);
}
