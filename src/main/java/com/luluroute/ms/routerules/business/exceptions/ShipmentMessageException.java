package com.luluroute.ms.routerules.business.exceptions;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShipmentMessageException extends Exception {
    String code;
    String description;
    String source;


    /** Call super so that ExceptionUtils always picks up detail fields **/
    public ShipmentMessageException(String code, String description, String source) {
        super(String.format("%s - %s", code, description));
        this.code = code;
        this.description = description;
        this.source = source;
    }
}
