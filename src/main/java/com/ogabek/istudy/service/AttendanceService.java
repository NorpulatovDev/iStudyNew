package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.BulkAttendanceRequest;
import com.ogabek.istudy.dto.request.MarkAttendanceRequest;
import com.ogabek.istudy.dto.response.AttendanceDto;
import com.ogabek.istudy.dto.response.BulkAttendanceResponse;
import com.ogabek.istudy.dto.response.StudentAttendanceSummaryDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final BranchRepository branchRepository;

    // NEW: Bulk attendance marking
    @Transactional
    public BulkAttendanceResponse markBulkAttendance(BulkAttendanceRequest request) {
        Group group = groupRepository.findByIdWithAllRelations(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + request.getGroupId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        List<AttendanceDto> savedAttendances = new ArrayList<>();
        int presentCount = 0;
        int absentCount = 0;

        for (BulkAttendanceRequest.StudentAttendanceItem item : request.getAttendances()) {
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + item.getStudentId()));

            // Check if attendance already exists for this date
            Optional<Attendance> existingAttendance = attendanceRepository.findByStudentAndGroupAndDate(
                    item.getStudentId(), request.getGroupId(), request.getAttendanceDate());

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                // Update existing attendance
                attendance = existingAttendance.get();
                attendance.setStatus(AttendanceStatus.valueOf(item.getStatus().toUpperCase()));
                attendance.setNote(item.getNote());
            } else {
                // Create new attendance
                attendance = new Attendance();
                attendance.setStudent(student);
                attendance.setGroup(group);
                attendance.setAttendanceDate(request.getAttendanceDate());
                attendance.setStatus(AttendanceStatus.valueOf(item.getStatus().toUpperCase()));
                attendance.setNote(item.getNote());
                attendance.setBranch(branch);
            }

            Attendance savedAttendance = attendanceRepository.save(attendance);
            savedAttendances.add(convertToDto(savedAttendance));

            // Count present/absent
            if (savedAttendance.getStatus() == AttendanceStatus.PRESENT) {
                presentCount++;
            } else {
                absentCount++;
            }
        }

        String message = String.format("Davomat muvaffaqiyatli saqlandi! Jami: %d, Kelgan: %d, Kelmagan: %d",
                savedAttendances.size(), presentCount, absentCount);

        return new BulkAttendanceResponse(
                group.getId(),
                group.getName(),
                request.getAttendanceDate(),
                savedAttendances.size(),
                presentCount,
                absentCount,
                savedAttendances,
                message
        );
    }

    // Keep existing methods...
    @Transactional
    public AttendanceDto markAttendance(MarkAttendanceRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + request.getStudentId()));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + request.getGroupId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        Optional<Attendance> existingAttendance = attendanceRepository.findByStudentAndGroupAndDate(
                request.getStudentId(), request.getGroupId(), request.getAttendanceDate());

        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
            attendance.setStatus(AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
            attendance.setNote(request.getNote());
        } else {
            attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setGroup(group);
            attendance.setAttendanceDate(request.getAttendanceDate());
            attendance.setStatus(AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
            attendance.setNote(request.getNote());
            attendance.setBranch(branch);
        }

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return convertToDto(savedAttendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByGroupAndDate(Long groupId, LocalDate date) {
        return attendanceRepository.findByGroupAndDate(groupId, date).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> getStudentAttendanceByMonth(Long studentId, Long groupId, int year, int month) {
        return attendanceRepository.findByStudentAndGroupAndMonth(studentId, groupId, year, month).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceSummaryDto> getGroupAttendanceSummary(Long groupId, int year, int month) {
        Group group = groupRepository.findByIdWithAllRelations(groupId)
                .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + groupId));

        List<StudentAttendanceSummaryDto> summaries = new ArrayList<>();

        if (group.getStudents() != null) {
            for (Student student : group.getStudents()) {
                int presentDays = attendanceRepository.countByStudentAndGroupAndMonthAndStatus(
                        student.getId(), groupId, year, month, AttendanceStatus.PRESENT);
                
                int absentDays = attendanceRepository.countByStudentAndGroupAndMonthAndStatus(
                        student.getId(), groupId, year, month, AttendanceStatus.ABSENT);

                int totalDays = presentDays + absentDays;
                double attendancePercentage = totalDays > 0 ? (presentDays * 100.0) / totalDays : 0.0;

                StudentAttendanceSummaryDto summary = new StudentAttendanceSummaryDto(
                        student.getId(),
                        student.getFirstName() + " " + student.getLastName(),
                        student.getPhoneNumber(),
                        presentDays,
                        absentDays,
                        totalDays,
                        Math.round(attendancePercentage * 100.0) / 100.0
                );

                summaries.add(summary);
            }
        }

        return summaries;
    }

    @Transactional
    public void deleteAttendance(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new RuntimeException("Davomat topilmadi: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    private AttendanceDto convertToDto(Attendance attendance) {
        AttendanceDto dto = new AttendanceDto();
        dto.setId(attendance.getId());

        if (attendance.getStudent() != null) {
            dto.setStudentId(attendance.getStudent().getId());
            dto.setStudentName(attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName());
        }

        if (attendance.getGroup() != null) {
            dto.setGroupId(attendance.getGroup().getId());
            dto.setGroupName(attendance.getGroup().getName());
        }

        dto.setAttendanceDate(attendance.getAttendanceDate());
        dto.setStatus(attendance.getStatus().name());
        dto.setNote(attendance.getNote());

        if (attendance.getBranch() != null) {
            dto.setBranchId(attendance.getBranch().getId());
            dto.setBranchName(attendance.getBranch().getName());
        }

        dto.setCreatedAt(attendance.getCreatedAt());
        return dto;
    }
}