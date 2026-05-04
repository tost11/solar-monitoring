package de.tostsoft.solarmonitoring.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerIpSecurityTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserController userController;

    @Test
    void getClientIpAddress_whenTrustForwardedHeadersFalse_shouldIgnoreXForwardedFor() throws Exception {
        ReflectionTestUtils.setField(userController, "trustForwardedHeaders", false);

        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        var method = UserController.class.getDeclaredMethod("getClientIpAddress", HttpServletRequest.class);
        method.setAccessible(true);
        String ip = (String) method.invoke(userController, request);

        assertThat(ip).isEqualTo("192.168.1.100");
    }

    @Test
    void getClientIpAddress_whenTrustForwardedHeadersTrue_shouldUseXForwardedFor() throws Exception {
        ReflectionTestUtils.setField(userController, "trustForwardedHeaders", true);

        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        lenient().when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        var method = UserController.class.getDeclaredMethod("getClientIpAddress", HttpServletRequest.class);
        method.setAccessible(true);
        String ip = (String) method.invoke(userController, request);

        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    void getClientIpAddress_whenTrustForwardedHeadersTrue_butHeaderMissing_shouldFallbackToRemoteAddr() throws Exception {
        ReflectionTestUtils.setField(userController, "trustForwardedHeaders", true);

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        var method = UserController.class.getDeclaredMethod("getClientIpAddress", HttpServletRequest.class);
        method.setAccessible(true);
        String ip = (String) method.invoke(userController, request);

        assertThat(ip).isEqualTo("192.168.1.100");
    }

    @Test
    void getClientIpAddress_whenXForwardedForHasMultipleIps_shouldUseFirst() throws Exception {
        ReflectionTestUtils.setField(userController, "trustForwardedHeaders", true);

        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");
        lenient().when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        var method = UserController.class.getDeclaredMethod("getClientIpAddress", HttpServletRequest.class);
        method.setAccessible(true);
        String ip = (String) method.invoke(userController, request);

        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    void getClientIpAddress_whenTrustForwardedHeadersFalse_attackerCannotSpoofIp() throws Exception {
        ReflectionTestUtils.setField(userController, "trustForwardedHeaders", false);

        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        var method = UserController.class.getDeclaredMethod("getClientIpAddress", HttpServletRequest.class);
        method.setAccessible(true);
        String ip = (String) method.invoke(userController, request);

        assertThat(ip).isEqualTo("192.168.1.100");
    }
}
