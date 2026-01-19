package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {
    @NotBlank(message = "Branch name is required!")
    private String name;
    private String address;
}
