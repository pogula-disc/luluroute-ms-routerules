package com.luluroute.ms.routerules.business.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.luluroute.ms.routerules.business.entity.BusinessRouteRulesEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class RouteRulesManageResponse {

    private HttpStatus status;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;
    private BusinessRouteRulesEntity data;
}
