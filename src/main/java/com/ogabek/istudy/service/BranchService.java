package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateBranchRequest;
import com.ogabek.istudy.dto.response.BranchDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.entity.Teacher;
import com.ogabek.istudy.entity.User;
import com.ogabek.istudy.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public List<BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));
        return convertToDto(branch);
    }

    public BranchDto createBranch(CreateBranchRequest request) {
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());

        Branch savedBranch = branchRepository.save(branch);
        return convertToDto(savedBranch);
    }

    public BranchDto updateBranch(Long id, CreateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());

        Branch savedBranch = branchRepository.save(branch);
        return convertToDto(savedBranch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + id));

        List<User> branchUsers = userRepository.findByBranchId(id);
        if (!branchUsers.isEmpty()) {
            throw new RuntimeException("Bu filialda " + branchUsers.size() + " ta foydalanuvchi mavjud. Avval foydalanuvchilarni boshqa filialga ko'chiring.");
        }

        List<Student> branchStudents = studentRepository.findByBranchId(id);
        if (!branchStudents.isEmpty()) {
            throw new RuntimeException("Bu filialda " + branchStudents.size() + " ta o'quvchi mavjud. Avval o'quvchilarni boshqa filialga ko'chiring.");
        }

        List<Teacher> branchTeachers = teacherRepository.findByBranchId(id);
        if (!branchTeachers.isEmpty()) {
            throw new RuntimeException("Bu filialda " + branchTeachers.size() + " ta o'qituvchi mavjud. Avval o'qituvchilarni boshqa filialga ko'chiring.");
        }

        try {
            branchRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Filialni o'chirishda xatolik yuz berdi: " + e.getMessage());
        }
    }

    private BranchDto convertToDto(Branch branch) {
        BranchDto dto = new BranchDto();
        dto.setId(branch.getId());
        dto.setName(branch.getName());
        dto.setAddress(branch.getAddress());
        dto.setCreatedAt(branch.getCreatedAt());
        return dto;
    }
}
