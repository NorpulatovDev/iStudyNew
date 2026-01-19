package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Teacher;
import com.ogabek.istudy.entity.SalaryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.branch WHERE t.branch.id = :branchId AND t.deleted = false ORDER BY t.createdAt ASC")
    List<Teacher> findByBranchIdWithBranch(@Param("branchId") Long branchId);

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.branch WHERE t.id = :id AND t.deleted = false")
    Optional<Teacher> findByIdWithBranch(@Param("id") Long id);

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.branch WHERE t.branch.id = :branchId AND t.deleted = false AND " +
            "(LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Teacher> findByBranchIdAndFullNameWithBranch(@Param("branchId") Long branchId, @Param("name") String name);

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.branch WHERE t.branch.id = :branchId AND t.deleted = false AND t.salaryType = :salaryType")
    List<Teacher> findByBranchIdAndSalaryTypeWithBranch(@Param("branchId") Long branchId, @Param("salaryType") SalaryType salaryType);

    @Query("SELECT t FROM Teacher t WHERE t.branch.id = :branchId AND t.deleted = false")
    List<Teacher> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT t FROM Teacher t WHERE t.branch.id = :branchId AND t.deleted = false AND " +
            "(LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Teacher> findByBranchIdAndFullName(@Param("branchId") Long branchId, @Param("name") String name);
}