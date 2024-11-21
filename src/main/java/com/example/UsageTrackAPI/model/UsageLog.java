package com.example.UsageTrackAPI.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @NotBlank(message = "Bin number is required")
    @Pattern(regexp = "^\\d{13}", message = "Bin number must be 13 digits")
    @Column(nullable = false)
    private String binNumber;

    @NotBlank(message = "SAD Number is required")
    @Column(nullable = false, length = 8)
    private String sadNumber;

    @NotBlank(message = "License Number is required")
    @Pattern(regexp = "^\\d{4}", message = "License number must be 4 digits")
    @Column(nullable = false)
    private String licenseCode;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Schema(hidden = true)
    private Status status;

    @Column(nullable = false)
    @Schema(hidden = true)
    private LocalDateTime usageDate;

}


