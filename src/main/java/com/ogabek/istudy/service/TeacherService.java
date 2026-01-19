package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateTeacherRequest;
import com.ogabek.istudy.dto.response.TeacherDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Group;
import com.ogabek.istudy.entity.SalaryType;
import com.ogabek.istudy.entity.Teacher;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.GroupRepository;
import com.ogabek.istudy.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<TeacherDto> getTeachersByBranch(Long branchId) {
        return teacherRepository.findByBranchIdWithBranch(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherDto> searchTeachersByName(Long branchId, String name) {
        return teacherRepository.findByBranchIdAndFullNameWithBranch(branchId, name).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherDto> getTeachersBySalaryType(Long branchId, String salaryType) {
        SalaryType salaryTypeEnum = SalaryType.valueOf(salaryType.toUpperCase());
        return teacherRepository.findByBranchIdAndSalaryTypeWithBranch(branchId, salaryTypeEnum)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherDto getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findByIdWithBranch(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));
        return convertToDto(teacher);
    }

    @Transactional
    public TeacherDto createTeacher(CreateTeacherRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        Teacher teacher = new Teacher();
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setPhoneNumber(request.getPhoneNumber());
        teacher.setBaseSalary(request.getBaseSalary());
        teacher.setPaymentPercentage(request.getPaymentPercentage());
        teacher.setSalaryType(request.getSalaryType());
        teacher.setBranch(branch);

        Teacher savedTeacher = teacherRepository.save(teacher);
        return convertToDto(savedTeacher);
    }

    @Transactional
    public TeacherDto updateTeacher(Long id, CreateTeacherRequest request) {
        Teacher teacher = teacherRepository.findByIdWithBranch(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setPhoneNumber(request.getPhoneNumber());
        teacher.setBaseSalary(request.getBaseSalary());
        teacher.setPaymentPercentage(request.getPaymentPercentage());
        teacher.setSalaryType(request.getSalaryType());
        teacher.setBranch(branch);

        Teacher savedTeacher = teacherRepository.save(teacher);
        return convertToDto(savedTeacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("O'qituvchi topilmadi: " + id));

        // Check if teacher has active (non-deleted) groups
        List<Group> teacherGroups = groupRepository.findByTeacherId(id);
        if (!teacherGroups.isEmpty()) {
            throw new RuntimeException("Bu o'qituvchida " + teacherGroups.size() +
                    " ta guruh mavjud. Avval guruhlarni boshqa o'qituvchiga tayinlang yoki o'chiring.");
        }

        // Soft delete by setting deleted flag
        teacher.setDeleted(true);
        teacherRepository.save(teacher);
    }

    private TeacherDto convertToDto(Teacher teacher) {
        TeacherDto dto = new TeacherDto();
        dto.setId(teacher.getId());
        dto.setFirstName(teacher.getFirstName());
        dto.setLastName(teacher.getLastName());
        dto.setPhoneNumber(teacher.getPhoneNumber());
        dto.setEmail(teacher.getEmail());
        dto.setBaseSalary(teacher.getBaseSalary());
        dto.setPaymentPercentage(teacher.getPaymentPercentage());
        dto.setSalaryType(teacher.getSalaryType().name());

        if (teacher.getBranch() != null) {
            dto.setBranchId(teacher.getBranch().getId());
            dto.setBranchName(teacher.getBranch().getName());
        }

        dto.setCreatedAt(teacher.getCreatedAt());
        return dto;
    }
}
