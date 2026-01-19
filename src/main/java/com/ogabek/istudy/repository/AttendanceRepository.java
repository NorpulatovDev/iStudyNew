package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Attendance;
import com.ogabek.istudy.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Find attendance by student, group, and date
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.group.id = :groupId AND a.attendanceDate = :date")
    Optional<Attendance> findByStudentAndGroupAndDate(@Param("studentId") Long studentId,
                                                       @Param("groupId") Long groupId,
                                                       @Param("date") LocalDate date);

    // Get all attendance for a group on specific date
    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.student " +
           "LEFT JOIN FETCH a.group " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.group.id = :groupId AND a.attendanceDate = :date")
    List<Attendance> findByGroupAndDate(@Param("groupId") Long groupId, @Param("date") LocalDate date);

    // Get student attendance for specific month
    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.student " +
           "LEFT JOIN FETCH a.group " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.student.id = :studentId AND a.group.id = :groupId " +
           "AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByStudentAndGroupAndMonth(@Param("studentId") Long studentId,
                                                    @Param("groupId") Long groupId,
                                                    @Param("year") int year,
                                                    @Param("month") int month);

    // Count present days for student in group for specific month
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.group.id = :groupId " +
           "AND a.status = :status AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month")
    int countByStudentAndGroupAndMonthAndStatus(@Param("studentId") Long studentId,
                                                @Param("groupId") Long groupId,
                                                @Param("year") int year,
                                                @Param("month") int month,
                                                @Param("status") AttendanceStatus status);

    // Get attendance for entire group for specific month
    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.student " +
           "WHERE a.group.id = :groupId " +
           "AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByGroupAndMonth(@Param("groupId") Long groupId,
                                        @Param("year") int year,
                                        @Param("month") int month);
}