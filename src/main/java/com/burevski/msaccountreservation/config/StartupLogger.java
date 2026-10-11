package com.burevski.msaccountreservation.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupLogger {

    @EventListener(ApplicationReadyEvent.class)
    public void logLinks(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext()
                instanceof ServletWebServerApplicationContext context)) {
            return;
        }

        var webServer = context.getWebServer();
        if (webServer == null) {
            return;
        }

        int port = webServer.getPort();
        String contextPath = context.getEnvironment()
                .getProperty("server.servlet.context-path", "");

        log.info("Swagger UI: http://localhost:{}{}/swagger-ui.html",
                port, contextPath);
    }
}
