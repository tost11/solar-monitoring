package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink.ShareLinkCreateDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink.ShareLinkResponseDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.sharelink.ShareResolveResponseDTO;
import de.tostsoft.solarmonitoring.lib.model.AccessToken;
import de.tostsoft.solarmonitoring.lib.model.ShareLink;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.ViewPermissionInfo;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.TokenPurpose;
import de.tostsoft.solarmonitoring.lib.repository.ShareLinkRepository;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.utils.PasswordValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.bson.types.ObjectId;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ShareLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(ShareLinkService.class);

    private static final String SECRET_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int GENERATED_SECRET_LENGTH = 12;
    private static final String PASSWORD_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#!%*?&";
    private static final int PASSWORD_LENGTH = 16;
    private static final String MINTED_TOKEN_NAME = "share-link";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String randomString(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(SECURE_RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }

    public ShareLinkResponseDTO create(String systemId, ShareLinkCreateDTO dto) {
        PublicMode level = PublicMode.valueOf(dto.getLevel());

        boolean manual = StringUtils.isNotBlank(dto.getSecret());
        String secret;
        if (manual) {
            secret = dto.getSecret().trim();
            if (!PasswordValidator.isValidPassword(secret)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "secret does not match the required password rules");
            }
        } else {
            secret = randomString(SECRET_CHARSET, GENERATED_SECRET_LENGTH);
        }

        // Secrets are stored bcrypt-hashed, so uniqueness is a hash scan over this system's links.
        for (ShareLink existing : shareLinkRepository.findBySystemId(systemId)) {
            if (passwordEncoder.matches(secret, existing.getSecretHash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "secret is already in use for this system");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        var link = ShareLink.builder()
                .id(new ObjectId().toString())
                .systemId(systemId)
                .secretHash(passwordEncoder.encode(secret))
                .manual(manual)
                .notes(dto.getNotes())
                .level(level.name())
                .remainingUsages(dto.getMaxUses())
                .expiresAt(dto.getExpiresAt())
                .tokenExpiresAt(dto.getTokenExpiresAt())
                .revoked(false)
                .createdAt(now)
                .build();
        shareLinkRepository.save(link);
        solarSystemRepository.addShareLink(systemId, link.getId());

        return Converter.convertShareLinkToShareLinkResponseDTO(link, secret);
    }

    public List<ShareLinkResponseDTO> list(String systemId) {
        return shareLinkRepository.findBySystemId(systemId).stream()
                .map(link -> Converter.convertShareLinkToShareLinkResponseDTO(link, null))
                .toList();
    }

    public ShareResolveResponseDTO resolve(String systemIdOrShortener, String secret) {
        ShareResolveResponseDTO invalid = ShareResolveResponseDTO.builder().valid(false).build();

        var systems = solarSystemRepository.findAllByIdOrShortener(systemIdOrShortener);
        if (systems.isEmpty()) {
            return invalid;
        }
        var system = systems.get(0);

        List<ShareLink> links = shareLinkRepository.findBySystemId(system.getId());
        // Always scan every hash (no early stop) so response time does not leak a match position.
        ShareLink matched = null;
        for (ShareLink link : links) {
            if (passwordEncoder.matches(secret, link.getSecretHash())) {
                matched = link;
            }
        }
        if (matched == null) {
            return invalid;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean unusable = matched.isRevoked()
                || (matched.getExpiresAt() != null && matched.getExpiresAt().isBefore(now))
                || (matched.getRemainingUsages() != null && matched.getRemainingUsages() <= 0);
        if (unusable) {
            return invalid;
        }

        if (matched.getRemainingUsages() != null) {
            int updated = shareLinkRepository.decrementRemainingUsages(matched.getId());
            if (updated == 0) {
                return invalid;
            }
        }

        String password = randomString(PASSWORD_CHARSET, PASSWORD_LENGTH);
        var token = AccessToken.builder()
                .id(UUID.randomUUID().toString())
                .name(MINTED_TOKEN_NAME)
                .hash(passwordEncoder.encode(password))
                .purpose(TokenPurpose.VIEW)
                .viewPermission(ViewPermissionInfo.builder()
                        .permission(PublicMode.valueOf(matched.getLevel()))
                        .linkId(matched.getId())
                        .build())
                .createdAt(now)
                .expiresAt(matched.getTokenExpiresAt())
                .build();
        solarSystemRepository.addToken(system.getId(), token);

        // targeted $set - saving the stale 'matched' entity would overwrite the atomic decrement
        shareLinkRepository.touchLastRedeemedAt(matched.getId(), now);

        LOG.info("Share link {} resolved for system {}", matched.getId(), system.getId());

        String name = displayName(system);
        return ShareResolveResponseDTO.builder()
                .valid(true)
                .systemId(system.getId())
                .level(matched.getLevel())
                .name(name)
                .expiresAt(token.getExpiresAt())
                .password(password)
                .build();
    }

    public void revoke(String systemId, String linkId) {
        var link = findOwnedLink(systemId, linkId);
        link.setRevoked(true);
        shareLinkRepository.save(link);
    }

    public void cleanup(SolarSystem system, String linkId) {
        var link = findOwnedLink(system.getId(), linkId);

        if (system.getTokens() != null) {
            for (AccessToken token : system.getTokens()) {
                if (token.getViewPermission() != null && linkId.equals(token.getViewPermission().getLinkId())) {
                    solarSystemRepository.removeToken(system.getId(), token.getId());
                }
            }
        }

        shareLinkRepository.delete(link);
        solarSystemRepository.removeShareLink(system.getId(), linkId);
    }

    private ShareLink findOwnedLink(String systemId, String linkId) {
        return shareLinkRepository.findByIdAndSystemId(linkId, systemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share link not found"));
    }

    private String displayName(SolarSystem system) {
        var info = system.getSystemInformations();
        if (info == null) {
            return null;
        }
        return StringUtils.isNotBlank(info.getPublicName()) ? info.getPublicName() : info.getViewName();
    }
}
