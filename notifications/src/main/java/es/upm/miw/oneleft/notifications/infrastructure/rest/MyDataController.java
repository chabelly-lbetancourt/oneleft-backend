package es.upm.miw.oneleft.notifications.infrastructure.rest;

import es.upm.miw.oneleft.notifications.domain.port.in.PersonalDataUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * My data in this service (HU-045). The users service gathers the data of all the services into the one file that
 * the person downloads.
 */
@RestController
@RequestMapping(MyDataController.MY_DATA)
@Tag(name = "My data", description = "Download my data (right of access and portability)")
public class MyDataController {

    public static final String MY_DATA = "/api/v1/notifications/me/data";

    private final PersonalDataUseCase personalData;

    public MyDataController(PersonalDataUseCase personalData) {
        this.personalData = personalData;
    }

    @GetMapping
    @Operation(summary = "What this service keeps of me",
            description = "Each table by name, as a list of rows. Only what is mine.")
    @ApiResponse(responseCode = "200", description = "My data in this service")
    public Map<String, List<Map<String, Object>>> myData(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return personalData.export(UUID.fromString(jwt.getSubject()));
    }
}
