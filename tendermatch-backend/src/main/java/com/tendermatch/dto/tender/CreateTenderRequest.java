package com.tendermatch.dto.tender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTenderRequest {

    @NotBlank(message = "Tender name is required")
    @Size(max = 500, message = "Name must not exceed 500 characters")
    private String name;

    private String description;

    public CreateTenderRequest() {}

    public CreateTenderRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
