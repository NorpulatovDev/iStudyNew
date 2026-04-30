package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateStudentRequest;
import com.ogabek.istudy.dto.response.GroupDto;
import com.ogabek.istudy.dto.response.PaymentDto;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.dto.response.UnpaidStudentDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Group;
import com.ogabek.istudy.entity.Payment;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.GroupRepository;
import com.ogabek.istudy.repository.PaymentRepository;
import com.ogabek.istudy.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId) {
        LocalDate now = LocalDate.now();
        List<Student> students = studentRepository.findByBranchIdWithBranch(branchId);
        return buildStudentDtosBulk(students, branchId, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        List<Student> students = studentRepository.findByBranchIdWithBranch(branchId);
        return buildStudentDtosBulk(students, branchId, targetYear, targetMonth);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByGroup(Long groupId, Integer year, Integer month) {
        Group group = groupRepository.findByIdWithAllRelations(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        if (group.getStudents() == null || group.getStudents().isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        Long branchId = group.getBranch().getId();
        return buildStudentDtosBulk(new ArrayList<>(group.getStudents()), branchId, targetYear, targetMonth);
    }

    @Transactional(readOnly = true)
    public List<UnpaidStudentDto> getUnpaidStudents(Long branchId, Integer year, Integer month) {
        List<Group> branchGroups = groupRepository.findByBranchIdWithStudents(branchId);

        Map<String, BigDecimal> paidMap = new HashMap<>();
        if (year == null || month == null) {
            for (Object[] row : paymentRepository.getAllPaymentTotalsPerStudentGroupForBranch(branchId)) {
                paidMap.put(row[0] + "_" + row[1], row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
            }
        } else {
            for (Object[] row : paymentRepository.getPaymentTotalsPerStudentGroupForMonth(branchId, year, month)) {
                paidMap.put(row[0] + "_" + row[1], row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
            }
        }

        List<UnpaidStudentDto> result = new ArrayList<>();
        for (Group group : branchGroups) {
            if (group.getStudents() == null) continue;
            for (Student student : group.getStudents()) {
                BigDecimal totalPaid = paidMap.getOrDefault(student.getId() + "_" + group.getId(), BigDecimal.ZERO);
                BigDecimal remaining = group.getPrice().subtract(totalPaid);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    result.add(new UnpaidStudentDto(
                            student.getId(),
                            student.getFirstName(),
                            student.getLastName(),
                            student.getPhoneNumber(),
                            student.getParentPhoneNumber(),
                            remaining,
                            group.getId(),
                            group.getName()));
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> searchStudentsByName(Long branchId, String name) {
        LocalDate now = LocalDate.now();
        List<Student> students = studentRepository.findByBranchIdAndFullName(branchId, name);
        return buildStudentDtosBulk(students, branchId, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getStudentPaymentHistory(Long studentId) {
        return paymentRepository.findByStudentIdWithRelations(studentId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getStudentGroups(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        return groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .map(this::convertGroupToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentStatistics(Long branchId) {
        List<Student> allStudents = studentRepository.findByBranchId(branchId);
        LocalDate now = LocalDate.now();
        List<Student> unpaidStudents = studentRepository.findUnpaidStudentsByBranchAndMonth(
                branchId, now.getYear(), now.getMonthValue());

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalStudents", allStudents.size());
        statistics.put("paidStudents", allStudents.size() - unpaidStudents.size());
        statistics.put("unpaidStudents", unpaidStudents.size());
        statistics.put("paymentRate",
                allStudents.size() > 0
                        ? (double) (allStudents.size() - unpaidStudents.size()) / allStudents.size() * 100
                        : 0);

        return statistics;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getRecentStudents(Long branchId, int limit) {
        LocalDate now = LocalDate.now();
        List<Student> students = studentRepository.findByBranchId(branchId).stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
        return buildStudentDtosBulk(students, branchId, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate now = LocalDate.now();
        return convertToDto(student, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id, Integer year, Integer month) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        return convertToDto(student, targetYear, targetMonth);
    }

    @Transactional
    public StudentDto createStudent(CreateStudentRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        Student student = new Student();
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);

        if (studentRepository.existsByBranchIdAndFirstNameAndLastNameAndPhoneNumberAndDeletedFalse(
                request.getBranchId(), request.getFirstName(), request.getLastName(), request.getPhoneNumber())) {
            throw new RuntimeException(
                    "O'quvchi allaqachon mavjud: " + request.getFirstName() + " " + request.getLastName());
        }

        Student savedStudent = studentRepository.save(student);

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            for (Long groupId : request.getGroupIds()) {
                Group group = groupRepository.findByIdWithAllRelations(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

                if (!group.getBranch().getId().equals(request.getBranchId())) {
                    throw new RuntimeException(
                            "Group " + groupId + " does not belong to branch " + request.getBranchId());
                }

                if (group.getStudents() == null) {
                    group.setStudents(new HashSet<>());
                }
                group.getStudents().add(savedStudent);
                groupRepository.save(group);
            }
        }

        LocalDate now = LocalDate.now();
        return convertToDto(savedStudent, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public StudentDto updateStudent(Long id, CreateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);

        Student savedStudent = studentRepository.save(student);

        List<Group> currentGroups = groupRepository.findByBranchIdWithAllRelations(branch.getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .collect(Collectors.toList());

        for (Group group : currentGroups) {
            group.getStudents().remove(student);
            groupRepository.save(group);
        }

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            for (Long groupId : request.getGroupIds()) {
                Group group = groupRepository.findByIdWithAllRelations(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

                if (!group.getBranch().getId().equals(request.getBranchId())) {
                    throw new RuntimeException(
                            "Group " + groupId + " does not belong to branch " + request.getBranchId());
                }

                if (group.getStudents() == null) {
                    group.setStudents(new HashSet<>());
                }
                group.getStudents().add(savedStudent);
                groupRepository.save(group);
            }
        }

        LocalDate now = LocalDate.now();
        return convertToDto(savedStudent, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + id));

        // Remove from all groups first
        List<Group> studentGroups = groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .collect(Collectors.toList());

        for (Group group : studentGroups) {
            group.getStudents().remove(student);
            groupRepository.save(group);
        }

        // Soft delete by setting deleted flag
        student.setDeleted(true);
        studentRepository.save(student);
    }

    private List<StudentDto> buildStudentDtosBulk(List<Student> students, Long branchId, int year, int month) {
        if (students.isEmpty()) return new ArrayList<>();

        List<Group> branchGroups = groupRepository.findByBranchIdWithStudents(branchId);
        Map<Long, List<Group>> studentGroupsMap = new HashMap<>();
        for (Group group : branchGroups) {
            if (group.getStudents() == null) continue;
            for (Student s : group.getStudents()) {
                studentGroupsMap.computeIfAbsent(s.getId(), k -> new ArrayList<>()).add(group);
            }
        }

        Map<Long, BigDecimal> totalPaidMap = new HashMap<>();
        Map<Long, Boolean> hasPaidMap = new HashMap<>();
        for (Object[] row : studentRepository.getMonthlyPaymentDataForBranch(branchId, year, month)) {
            Long sid = (Long) row[0];
            totalPaidMap.put(sid, row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
            hasPaidMap.put(sid, ((Long) row[2]) > 0);
        }

        Map<Long, LocalDateTime> lastPaymentMap = new HashMap<>();
        for (Object[] row : studentRepository.getLastPaymentDatesForBranch(branchId)) {
            lastPaymentMap.put((Long) row[0], (LocalDateTime) row[1]);
        }

        Map<Long, BigDecimal> expectedMap = new HashMap<>();
        for (Object[] row : studentRepository.getExpectedPaymentsForBranch(branchId)) {
            expectedMap.put((Long) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        return students.stream()
                .map(s -> convertToDtoBulk(s, studentGroupsMap, totalPaidMap, hasPaidMap, lastPaymentMap, expectedMap))
                .collect(Collectors.toList());
    }

    private StudentDto convertToDtoBulk(Student student,
                                         Map<Long, List<Group>> studentGroupsMap,
                                         Map<Long, BigDecimal> totalPaidMap,
                                         Map<Long, Boolean> hasPaidMap,
                                         Map<Long, LocalDateTime> lastPaymentMap,
                                         Map<Long, BigDecimal> expectedMap) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setParentPhoneNumber(student.getParentPhoneNumber());
        dto.setCreatedAt(student.getCreatedAt());

        if (student.getBranch() != null) {
            dto.setBranchId(student.getBranch().getId());
            dto.setBranchName(student.getBranch().getName());
        }

        List<Group> groups = studentGroupsMap.getOrDefault(student.getId(), new ArrayList<>());
        dto.setGroups(groups.stream()
                .map(g -> new StudentDto.GroupInfo(
                        g.getId(),
                        g.getName(),
                        g.getPrice(),
                        g.getTeacher() != null
                                ? g.getTeacher().getFirstName() + " " + g.getTeacher().getLastName()
                                : null))
                .collect(Collectors.toList()));

        Long sid = student.getId();
        BigDecimal totalPaid = totalPaidMap.getOrDefault(sid, BigDecimal.ZERO);
        BigDecimal expected = expectedMap.getOrDefault(sid, BigDecimal.ZERO);
        BigDecimal remaining = expected.subtract(totalPaid);

        dto.setHasPaidInMonth(hasPaidMap.getOrDefault(sid, false));
        dto.setTotalPaidInMonth(totalPaid);
        dto.setRemainingAmount(remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);
        dto.setLastPaymentDate(lastPaymentMap.get(sid));

        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            dto.setPaymentStatus("UNPAID");
        } else if (totalPaid.compareTo(expected) >= 0) {
            dto.setPaymentStatus("PAID");
        } else {
            dto.setPaymentStatus("PARTIAL");
        }

        return dto;
    }

    private StudentDto convertToDto(Student student, int year, int month) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setParentPhoneNumber(student.getParentPhoneNumber());

        if (student.getBranch() != null) {
            dto.setBranchId(student.getBranch().getId());
            dto.setBranchName(student.getBranch().getName());
        }

        dto.setCreatedAt(student.getCreatedAt());

        if (student.getBranch() != null) {
            List<Group> studentGroups = groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId())
                    .stream()
                    .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                    .collect(Collectors.toList());

            List<StudentDto.GroupInfo> groupInfos = studentGroups.stream()
                    .map(group -> {
                        String teacherName = group.getTeacher() != null
                                ? group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName()
                                : null;
                        return new StudentDto.GroupInfo(
                                group.getId(),
                                group.getName(),
                                group.getPrice(),
                                teacherName);
                    })
                    .collect(Collectors.toList());

            dto.setGroups(groupInfos);
        }

        calculatePaymentStatus(dto, student.getId(), year, month);

        return dto;
    }

    private void calculatePaymentStatus(StudentDto dto, Long studentId, int year, int month) {
        Boolean hasPaid = studentRepository.hasStudentPaidInMonth(studentId, year, month);
        dto.setHasPaidInMonth(hasPaid != null ? hasPaid : false);

        BigDecimal totalPaid = studentRepository.getTotalPaidByStudentInMonth(studentId, year, month);
        dto.setTotalPaidInMonth(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        BigDecimal expectedPayment = studentRepository.getExpectedMonthlyPaymentForStudent(studentId);
        expectedPayment = expectedPayment != null ? expectedPayment : BigDecimal.ZERO;

        BigDecimal remaining = expectedPayment.subtract(dto.getTotalPaidInMonth());
        dto.setRemainingAmount(remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);

        if (dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) == 0) {
            dto.setPaymentStatus("UNPAID");
        } else if (dto.getTotalPaidInMonth().compareTo(expectedPayment) >= 0) {
            dto.setPaymentStatus("PAID");
        } else {
            dto.setPaymentStatus("PARTIAL");
        }

        LocalDateTime lastPaymentDate = studentRepository.getLastPaymentDate(studentId);
        dto.setLastPaymentDate(lastPaymentDate);
    }

    private PaymentDto convertPaymentToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        if (payment.getStudent() != null) {
            dto.setStudentId(payment.getStudent().getId());
            dto.setStudentName(payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName());
        }
        if (payment.getGroup() != null) {
            dto.setGroupId(payment.getGroup().getId());
            dto.setGroupName(payment.getGroup().getName());
        }
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setStatus(payment.getStatus().name());
        if (payment.getBranch() != null) {
            dto.setBranchId(payment.getBranch().getId());
            dto.setBranchName(payment.getBranch().getName());
        }
        dto.setPaymentYear(payment.getPaymentYear());
        dto.setPaymentMonth(payment.getPaymentMonth());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private GroupDto convertGroupToDto(Group group) {
        GroupDto dto = new GroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setPrice(group.getPrice());

        if (group.getTeacher() != null) {
            dto.setTeacherId(group.getTeacher().getId());
            dto.setTeacherName(group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName());
        }

        if (group.getBranch() != null) {
            dto.setBranchId(group.getBranch().getId());
            dto.setBranchName(group.getBranch().getName());
        }

        dto.setStartTime(group.getStartTime());
        dto.setEndTime(group.getEndTime());

        if (group.getDaysOfWeek() != null && !group.getDaysOfWeek().isEmpty()) {
            dto.setDaysOfWeek(Arrays.asList(group.getDaysOfWeek().split(",")));
        } else {
            dto.setDaysOfWeek(new ArrayList<>());
        }

        dto.setCreatedAt(group.getCreatedAt());
        return dto;
    }
}
