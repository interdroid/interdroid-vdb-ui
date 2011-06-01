/*
Copyright (c) 2008-2009 Vrije Universiteit, The Netherlands
All rights reserved.

Redistribution and use in source and binary forms,
with or without modification, are permitted provided
that the following conditions are met:

   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.

   * Neither the name of the Vrije Universiteit nor the names of its
     contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package interdroid.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * A utility library for doing various cryptography related things with sensible defaults.
 * Uses java.security and javax.crypto packages and deals only in Strings.
 *
 * @author nick palmer@cs.vu.nl
 *
 */
public class CryptoUtil {

    /**
     * The default encoding for String->byte[] and byte[]->String to use.
     */
    public static final String DEFAULT_ENCODING = "UTF8";
    /**
     * The default MAC generation scheme
     */
    public static final String DEFAULT_MAC = "HmacMD5";
    /**
     * The default Digest generation scheme
     */
    public static final String DEFAULT_DIGEST = "MD5";
    /**
     * The default cypher to use for secret key encryption/decryption
     */
    public static final String DEFAULT_CYPHER = "Blowfish";
    /**
     * The default mode to use for secret key encryption/decryption
     */
    public static final String DEFAULT_MODE = "ECB";
    /**
     * The default padding scheme to use for secret key encryption/decryption
     */
    public static final String DEFAULT_PADDING = "PKCS5Padding";
    /**
     * The default public key encryption scheme
     */
    public static final String DEFAULT_KEY_PAIR_TYPE = "RSA";
    /**
     * The default signature scheme
     */
    public static final String DEFAULT_SIGNATURE_SPEC = "MD5WithRSA";

    /**
     * The default cypher specification which is the combination of the DEFAULT_CYPHER, DEFAULT_MODE and DEFAULT_PADDING
     */
    public static final String DEFAULT_CYPHER_SPEC = DEFAULT_CYPHER + "/" + DEFAULT_MODE + "/" + DEFAULT_PADDING;

    /**
     * The default key size for public key encryption
     */
    public static final int DEFAULT_KEY_PAIR_SIZE = 2048;

    // Signature stuff

    /**
     * Generates a signature for the given plainTextString using the PrivateKey key and the signature type and
     * string encoding scheme specified.
     *
     * @param plainTextString The text to be signed
     * @param key The private key to use to sign
     * @param type The signature specification
     * @param encoding The string encoding method
     * @return The generated signature
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @throws NoSuchAlgorithmException if the type is not supported
     * @throws InvalidKeyException if the private key is invalid
     * @throws SignatureException if there is a problem generating the signature
     */
    public String generateSignatureTypeEncoded(String plainTextString, PrivateKey key, String type, String encoding)
    throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] plainText = plainTextString.getBytes(encoding);

        Signature sig = Signature.getInstance(type);
        sig.initSign(key);
        sig.update(plainText);

        byte[] signature = sig.sign();

        return new String(signature, encoding);
    }


    /**
     * Generates a signature for the given plain text using the private key and the DEFAULT_SIGNATURE_SPEC and DEFAULT_ENCODING
     *
     * @param plainTextString The text to sign
     * @param key The key to use to generate the signature
     * @return The signature
     * @throws InvalidKeyException If the key is invalid
     * @throws UnsupportedEncodingException If the DEFAULT_ENCODING is not supported
     * @throws NoSuchAlgorithmException If the DEFAULT_SIGNATURE_SPEC is not supported
     * @throws SignatureException If there is a problem generating the signature
     */
    public String generateSignature(String plainTextString, PrivateKey key)
    throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, SignatureException {
        return generateSignatureTypeEncoded(plainTextString, key, DEFAULT_SIGNATURE_SPEC, DEFAULT_ENCODING);
    }

    /**
     * Verifies the signature for the given plain text using the specified key, signature scheme and string encoding
     *
     * @param plainTextString The plain text to be verified
     * @param signatureString The signature to be checked
     * @param key The public key to verify with
     * @param type The signature scheme being used
     * @param encoding The string encoding scheme to use
     * @throws NoSuchAlgorithmException If the signature type is not supported
     * @throws UnsupportedEncodingException If the encoding type is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws SignatureException If there is a problem verifying the signature
     */
    public void verifySignatureTypeEncoded(String plainTextString, String signatureString, PublicKey key, String type, String encoding)
    throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, SignatureException {
        byte[] plainText = plainTextString.getBytes(encoding);
        byte[] signature = signatureString.getBytes(encoding);

        Signature sig = Signature.getInstance(type);
        sig.initVerify(key);
        sig.update(plainText);
        sig.verify( signature );

    }

    /**
     * Verifies the signature on the given plain text using the specified key using the DEFAULT_SIGNATURE_SPEC and the DEFAULT_ENCODING
     *
     * @param plainTextString The plain text to be verified
     * @param signature  The signature to be checked
     * @param key The public key to verify with
     * @throws NoSuchAlgorithmException If the DEFAULT_SIGNATURE_SPEC is not supported
     * @throws UnsupportedEncodingException If the DEFAULT_ENCODING is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws SignatureException If there is a problem verifying the signature
     */
    public void verifySignature(String plainTextString, String signature, PublicKey key)
    throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException {
        verifySignatureTypeEncoded(plainTextString, signature, key, DEFAULT_SIGNATURE_SPEC, DEFAULT_ENCODING);
    }

    // Secret Key Encryption and Decryption

    /**
     * Decrypt using the DEFAULT_CYPHER_SPEC and the DEFAULT_ENCODING
     *
     * @param plainTextString the plain text to encrypt
     * @param key The secret key to do the encoding with
     * @throws NoSuchPaddingException If the DEFAULT_PADDING is not supported
     * @throws NoSuchAlgorithmException If the DEFAULT_CYPHER not supported
     * @throws UnsupportedEncodingException If the DEFAULT_ENCODING is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws BadPaddingException If the padding is incorrect for the DEFAULT_PADING
     * @throws IllegalBlockSizeException If the block size is incorrect while decrypting
     * @return the decrypted text
     */
    public static String decrypt(String plainTextString, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(DEFAULT_CYPHER_SPEC);
        byte[] plainText = plainTextString.getBytes(DEFAULT_ENCODING);

        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(plainText), DEFAULT_ENCODING);
    }

    /**
     * Encrypt using the DEFAULT_CYPHER_SPEC and the DEFAULT_ENCODING
     *
     * @param plainTextString the plain text to encrypt
     * @param key The secret key to do the encoding with
     * @throws NoSuchPaddingException If the DEFAULT_PADDING is not supported
     * @throws NoSuchAlgorithmException If the DEFAULT_CYPHER is not supported
     * @throws UnsupportedEncodingException If the DEFAULT_ENCODING is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws BadPaddingException If the padding is incorrect
     * @throws IllegalBlockSizeException If there is a block size problem
     * @return the encrypted text
     */
    public static String encrypt(String plainTextString, SecretKey key)
    throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(DEFAULT_CYPHER);
        byte[] plainText = plainTextString.getBytes(DEFAULT_ENCODING);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        return new String(cipher.doFinal(plainText), DEFAULT_ENCODING);
    }

    // Key Methods

    /**
     * Generates a KeyPair with the given type and given size
     *
     * @param type The type of key pair to generate
     * @param size The size of the key pair to generate
     * @return The generated key pair
     * @throws NoSuchAlgorithmException if the type is not supported
     */
    public static KeyPair generateKeyPairTypeSize(String type, int size) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(type);
        keyGen.initialize(size);

        return keyGen.generateKeyPair();
    }

    /**
     * Generates a KeyPair with the DEFAULT_KEY_PAIR_TYPE and the DEFAULT_KEY_PAIR_SIZE
     *
     * @return The generated key pair
     * @throws NoSuchAlgorithmException If the DEFAULT_KEY_PAIR_TYPE is not supported
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPairTypeSize(DEFAULT_KEY_PAIR_TYPE, DEFAULT_KEY_PAIR_SIZE);
    }

    /**
     * Generates a new secret key of the requested type
     *
     * @param type A key type like "HmacMD5" or "DES"
     * @param keysize The size of the key to generate
     * @return a new SecretKey of the given type and size
     * @throws NoSuchAlgorithmException If teh type is not supported
     */
    public static SecretKey generateSecretKeySized(String type, int keysize)
    throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(type);
        keyGen.init( keysize );
        SecretKey key = keyGen.generateKey();
        return key;
    }

    /**
     * Generates a new secret key of the requested type
     *
     * @param type A key type like "HmacMD5"
     * @return a new SecretKey of the given type
     * @throws NoSuchAlgorithmException If the type is not supported
     */
    public static SecretKey generateSecretKey(String type)
    throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(type);
        SecretKey key = keyGen.generateKey();
        return key;
    }

    // MAC methods

    /**
     * Generates a MAC using the specified type and encoding
     *
     * @param plainTextString The plain text to generate a MAC for
     * @param key The key to use to generate the MAC
     * @param type The type of algorithm to use
     * @param encoding The encoding scheme to use
     * @return The MAC for the plain text
     * @throws NoSuchAlgorithmException If the type is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws UnsupportedEncodingException If the encoding is not supported
     */
    public static String getMacTypeEncoded(String plainTextString, SecretKey key, String type, String encoding)
    throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Mac mac = Mac.getInstance(type);
        mac.init(key);
        byte[] plainText = plainTextString.getBytes(encoding);
        mac.update(plainText);

        return new String(mac.doFinal(), encoding);
    }

    /**
     * Generates a MAC using the DEFAULT_MAC and the DEFAULT_ENCODING
     *
     * @param plainTextString The plain text to generate a MAC for
     * @param key The key to use to generate the MAC
     * @return The MAC for the given plain text
     * @throws NoSuchAlgorithmException If the DEFAULT_MAC is not supported
     * @throws InvalidKeyException If the key is invalid
     * @throws UnsupportedEncodingException If the DEFAULT_ENCODING is not supported
     */
    public static String getMac(String plainTextString, SecretKey key)
    throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return getMacTypeEncoded(plainTextString, key, DEFAULT_MAC, DEFAULT_ENCODING);
    }

    // Digest Methods

    /**
     * Generates a digest using the specified digest and encoding
     *
     * @param plainTextString
     * @param type
     * @param encoding
     * @return the digest as a string
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public static String getDigestTypeEncoded(String plainTextString, String type, String encoding)
    throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] plainText = plainTextString.getBytes(encoding);

        MessageDigest messageDigest = MessageDigest.getInstance(type);
        messageDigest.update( plainText);

        return new String( messageDigest.digest(), encoding);
    }

    /**
     * Generates a digest using the DEFAULT_DIGEST and the DEFAULT_ENCODING
     *
     * @param plainTextString
     * @return the digest as a string
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public static String getDigest(String plainTextString)
    throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return getDigestTypeEncoded(plainTextString, DEFAULT_DIGEST, DEFAULT_ENCODING);
    }

}
