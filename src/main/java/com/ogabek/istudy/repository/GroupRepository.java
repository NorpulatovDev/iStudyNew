package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "WHERE g.branch.id = :branchId AND g.deleted = false")
    List<Group> findByBranchIdWithAllRelations(@Param("branchId") Long branchId);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "LEFT JOIN FETCH g.students " +
            "WHERE g.id = :groupId AND g.deleted = false")
    Optional<Group> findByIdWithAllRelations(@Param("groupId") Long groupId);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.students " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "WHERE g.id = :groupId AND g.deleted = false")
    Group findByIdWithStudents(@Param("groupId") Long groupId);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "WHERE g.teacher.id = :teacherId AND g.deleted = false")
    List<Group> findByTeacherIdWithRelations(@Param("teacherId") Long teacherId);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "WHERE g.branch.id = :branchId AND g.teacher.id = :teacherId AND g.deleted = false")
    List<Group> findByBranchIdAndTeacherIdWithRelations(@Param("branchId") Long branchId, @Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "JOIN g.students s " +
            "WHERE g.branch.id = :branchId AND g.deleted = false AND " +
            "s.id NOT IN (SELECT DISTINCT p.student.id FROM Payment p WHERE p.paymentYear = :year AND p.paymentMonth = :month)")
    List<Group> findGroupsWithUnpaidStudentsWithRelations(@Param("branchId") Long branchId,
                                                          @Param("year") int year, @Param("month") int month);

    @Query("SELECT g FROM Group g WHERE g.branch.id = :branchId AND g.deleted = false")
    List<Group> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT g FROM Group g WHERE g.teacher.id = :teacherId AND g.deleted = false")
    List<Group> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT g FROM Group g JOIN g.students s WHERE g.branch.id = :branchId AND g.deleted = false AND " +
            "s.id NOT IN (SELECT DISTINCT p.student.id FROM Payment p WHERE p.paymentYear = :year AND p.paymentMonth = :month)")
    List<Group> findGroupsWithUnpaidStudents(@Param("branchId") Long branchId,
                                             @Param("year") int year, @Param("month") int month);

    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.teacher " +
            "LEFT JOIN FETCH g.branch " +
            "WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')) AND g.branch.id = :branchId AND g.deleted = false")
    List<Group> findByBranchIdAndNameContainingIgnoreCase(@Param("branchId") Long branchId, @Param("name") String name);
}