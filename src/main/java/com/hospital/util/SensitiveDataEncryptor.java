package com.hospital.util;

import com.hospital.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 敏感数据加解密工具
 */
@Slf4j
@Component
public class SensitiveDataEncryptor {

    private final SecurityProperties securityProperties;
    private SecretKeySpec secretKeySpec;

    private static SensitiveDataEncryptor INSTANCE;

    public SensitiveDataEncryptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = deriveKey(securityProperties.getSensitiveKey());
            this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            INSTANCE = this;
            log.info("SensitiveDataEncryptor initialized with AES key length={}", keyBytes.length);
        } catch (Exception e) {
            throw new IllegalStateException("初始化敏感数据加解密组件失败", e);
        }
    }

    private byte[] deriveKey(String rawKey) throws GeneralSecurityException {
        if (!StringUtils.hasText(rawKey)) {
            throw new GeneralSecurityException("敏感数据密钥未配置");
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hashed = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hashed, 0, key, 0, 16);
        return key;
    }

    public static SensitiveDataEncryptor getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SensitiveDataEncryptor尚未初始化");
        }
        return INSTANCE;
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("敏感数据加密失败", e);
            throw new IllegalStateException("敏感数据加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("敏感数据解密失败，返回原始密文", e);
            return cipherText;
        }
    }

    public String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}


