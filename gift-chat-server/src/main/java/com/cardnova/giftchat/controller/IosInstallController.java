package com.cardnova.giftchat.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class IosInstallController {

    private static final MediaType MOBILE_CONFIG_MEDIA_TYPE =
        MediaType.parseMediaType("application/x-apple-aspen-config");

    private static final String MOBILE_CONFIG = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
            <key>PayloadContent</key>
            <array>
                <dict>
                    <key>FullScreen</key>
                    <true/>
                    <key>IsRemovable</key>
                    <true/>
                    <key>Label</key>
                    <string>Xcard</string>
                    <key>PayloadDescription</key>
                    <string>Adds Xcard to the Home Screen.</string>
                    <key>PayloadDisplayName</key>
                    <string>Xcard</string>
                    <key>PayloadIdentifier</key>
                    <string>com.xcard.install.webclip</string>
                    <key>PayloadType</key>
                    <string>com.apple.webClip.managed</string>
                    <key>PayloadUUID</key>
                    <string>57C4DEED-EFDA-48E7-8D34-C7D9B7F2C89A</string>
                    <key>PayloadVersion</key>
                    <integer>1</integer>
                    <key>Precomposed</key>
                    <true/>
                    <key>URL</key>
                    <string>https://stonetradex.com</string>
                </dict>
            </array>
            <key>PayloadDescription</key>
            <string>Installs the Xcard Home Screen shortcut.</string>
            <key>PayloadDisplayName</key>
            <string>Install Xcard</string>
            <key>PayloadIdentifier</key>
            <string>com.xcard.install.profile</string>
            <key>PayloadOrganization</key>
            <string>Xcard</string>
            <key>PayloadRemovalDisallowed</key>
            <false/>
            <key>PayloadType</key>
            <string>Configuration</string>
            <key>PayloadUUID</key>
            <string>E7175391-1299-4B24-A3B6-16B6A2407FA2</string>
            <key>PayloadVersion</key>
            <integer>1</integer>
        </dict>
        </plist>
        """;

    @GetMapping("/api/install/ios-profile")
    public ResponseEntity<byte[]> downloadProfile() {
        return ResponseEntity.ok()
            .contentType(MOBILE_CONFIG_MEDIA_TYPE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Xcard.mobileconfig\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .body(MOBILE_CONFIG.getBytes(StandardCharsets.UTF_8));
    }
}
