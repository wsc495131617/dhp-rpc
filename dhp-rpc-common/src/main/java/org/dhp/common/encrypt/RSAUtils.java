package org.dhp.common.encrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtils {
	
	static Logger logger = LoggerFactory.getLogger(RSAUtils.class);
	
	public static String[] makeBase64Keys() {
		
		KeyPair keyPair = makeKeyPair(1024);
		
		String[] keys = new String[2];
		
        // 得到公钥  
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        try {
			keys[0] = Base64.getEncoder().encodeToString(publicKey.getEncoded());
			
			keys[1] = Base64.getEncoder().encodeToString(privateKey.getEncoded());
			
		} catch (Exception e) {
		}
        
        return keys;
        
	}
	
	/**
	 * 秘钥长度：96-1024
	 * @param size
	 * @return
	 */
	protected static KeyPair makeKeyPair(int size) {
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象  
        KeyPairGenerator keyPairGen = null;  
        try {  
            keyPairGen = KeyPairGenerator.getInstance("RSA");  
        } catch (NoSuchAlgorithmException e) {  
        		logger.error(e.getMessage(),e);
        		return null;
        }  
        // 初始化密钥对生成器，密钥大小为96-1024位  
        keyPairGen.initialize(size,new SecureRandom());  
        // 生成一个密钥对，保存在keyPair中  
        KeyPair keyPair = keyPairGen.generateKeyPair();  
        return keyPair;
	}
	
	public static byte[] encodeByPublicKey(byte[] body,String publicKeyStr) {
		try {
			byte[] buffer = Base64.getDecoder().decode(publicKeyStr);  
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
	        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);  
	        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
	        return encrypt(publicKey, body);	
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static byte[] decodeByPrivateKey(byte[] body,String privateKeyStr) {
		try {
			byte[] buffer = Base64.getDecoder().decode(privateKeyStr);  
	        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);  
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
	        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	        return decrypt(privateKey, body);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * 加密
	 * @param publicKey
	 * @param srcBytes
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private static byte[] encrypt(RSAPublicKey publicKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		if(publicKey!=null){
			//Cipher负责完成加密或解密工作，基于RSA
			Cipher cipher = Cipher.getInstance("RSA");
			//根据公钥，对Cipher对象进行初始化
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] resultBytes = cipher.doFinal(srcBytes);
			return resultBytes;
		}
		return null;
	}
	
	/**
	 * 解密 
	 * @param privateKey
	 * @param srcBytes
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private static byte[] decrypt(RSAPrivateKey privateKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		if(privateKey!=null){
			//Cipher负责完成加密或解密工作，基于RSA
			Cipher cipher = Cipher.getInstance("RSA");
			//根据公钥，对Cipher对象进行初始化
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] resultBytes = cipher.doFinal(srcBytes);
			return resultBytes;
		}
		return null;
	}
	
}
