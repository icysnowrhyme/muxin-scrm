package com.muxin.scrm.module.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.muxin.scrm.core.exception.BusinessException;
import com.muxin.scrm.module.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiCryptoService {

    private final AiProperties properties;

    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return null;
        }
        return cipher().encryptBase64(plainText);
    }

    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return null;
        }
        try {
            return cipher().decryptStr(cipherText);
        } catch (Exception ex) {
            throw new BusinessException("AI 密钥解密失败，请重新保存 API Key");
        }
    }

    private cn.hutool.crypto.symmetric.AES cipher() {
        String secret = StrUtil.blankToDefault(properties.getEncryptKey(), "muxin-local-ai-key");
        return SecureUtil.aes(DigestUtil.sha256(secret));
    }
}
