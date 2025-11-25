package com.hospital.util;

import com.hospital.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataEncryptorTest {

    private SensitiveDataEncryptor encryptor;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.setSensitiveKey("UnitTestKey123456");
        encryptor = new SensitiveDataEncryptor(properties);
        encryptor.init();
    }

    @Test
    void encryptAndDecrypt_shouldReturnOriginalText() {
        String plain = "13812345678";

        String cipher = encryptor.encrypt(plain);
        String decrypted = encryptor.decrypt(cipher);

        assertThat(cipher).isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void maskingHelpers_shouldHideSensitiveParts() {
        assertThat(encryptor.maskPhone("13812345678")).isEqualTo("138****5678");
        assertThat(encryptor.maskIdCard("320104199912123456")).isEqualTo("3201********3456");
    }
}


