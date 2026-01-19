package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BranchDto {
    private Long id;
    private String name;
    private String address;
    private LocalDateTime createdAt;
}
