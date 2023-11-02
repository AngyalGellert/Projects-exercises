package hu.progmasters.hotel.dto.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class HotelCreateRequest {

    @NotNull(message = "Hotel name must not be empty")
    @Size(min = 1, max = 200, message = "Hotel name must be between 1 and 200 characters")
    private String name;

    @NotNull(message = "Hotel address must not be empty")
    @Size(min = 1, max = 200, message = "Hotel address must be between 1 and 200 characters")
    private String address;


}
