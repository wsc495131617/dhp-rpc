package org.dhp.common.encrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptUtils   
{  
	
	/** 
	 * DES          key size must be equal to 56 
	 * DESede(TripleDES) key size must be equal to 112 or 168 
	 * AES          key size must be equal to 128, 192 or 256,but 192 and 256 bits may not be available 
	 * Blowfish     key size must be multiple of 8, and can only range from 32 to 448 (inclusive) 
	 * RC2          key size must be between 40 and 1024 bits 
	 * RC4(ARCFOUR) key size must be between 40 and 1024 bits 
	 **/ 
	protected static String EncrpytType = "AES"; 
	
    public static byte[] desEncrypt(byte[] plainText,String tag) throws Exception  
    {  
        SecureRandom sr = new SecureRandom();  
        DESKeySpec dks = new DESKeySpec(tag.getBytes());  
        //只试用DES
//        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");  
//        SecretKey key = keyFactory.generateSecret(dks);  
        
        //当使用其他对称加密算法时，如AES、Blowfish等算法时，用下述代码替换上述三行代码
        SecretKey key = new SecretKeySpec(tag.getBytes(),EncrpytType);
        		
        		
        Cipher cipher = Cipher.getInstance(EncrpytType);  
        cipher.init(Cipher.ENCRYPT_MODE, key, sr);  
        byte data[] = plainText;  
        byte encryptedData[] = cipher.doFinal(data);  
        return encryptedData;  
    }  
      
    public static byte[] desDecrypt(byte[] encryptText,String tag) throws Exception   
    {  
    	SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(tag.getBytes());

        SecretKey key = new SecretKeySpec(tag.getBytes(), EncrpytType);

        Cipher cipher = Cipher.getInstance(EncrpytType);
        cipher.init(2, key, sr);
        byte[] encryptedData = encryptText;
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return decryptedData;
    }  
      
    public static String encrypt(String input,String tag) throws Exception  
    {  
        return base64Encode(desEncrypt(input.getBytes(),tag));  
    }  
      
    public static String decrypt(String input,String tag) throws Exception   
    {  
        byte[] result = base64Decode(input);  
        return new String(desDecrypt(result,tag));  
    }  
      
    public static String base64Encode(byte[] s)   
    {  
        if (s == null)  
            return null;  
        return Base64.getEncoder().encodeToString(s);
    }  
      
    public static byte[] base64Decode(String s) throws IOException   
    {  
        if (s == null)  
        {  
           return null;  
        }  
        byte[] b = Base64.getDecoder().decode(s);  
        return b;  
    }  
}  

