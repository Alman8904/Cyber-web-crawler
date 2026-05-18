package com.supply.supplyTrace.site;

import jakarta.validation.constraints.NotBlank;

public record CreateSiteRequest(
        @NotBlank(message = "Site URL is required") String url
) {
}





