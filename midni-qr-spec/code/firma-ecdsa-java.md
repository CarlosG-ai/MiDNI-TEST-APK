# Verificación de firma ECDSA (Java)

```java
public static void main(String[] args) {     try {

//

// Certificado de firma

```java
// 
        String signerPemCert = 
            "MIIIPDCCBiSgAwIBAgIQInSUgkC5No9l5cgP6/5c5DANBgkqhkiG9w0BAQsFADB0MQswCQYDVQQGEwJF"+ 
            "UzEoMCYGA1UECgwfRElSRUNDSU9OIEdFTkVSQUwgREUgTEEgUE9MSUNJQTEMMAoGA1UECwwDQ05QMRgw"+             "FgYDVQRhDA9WQVRFUy1TMjgxNjAxNUgxEzARBgNVBAMMCkFDIERHUCAwMDQwHhcNMjQwMzA0MTMwOTM1"+             "WhcNMjkwMzA0MTMwOTM1WjCBozELMAkGA1UEBhMCRVMxIDAeBgNVBAoTF01JTklTVEVSSU8gREVMIElO"+             "VEVSSU9SMRowGAYDVQQLExFTRUxMTyBFTEVDVFJPTklDTzEjMCEGA1UECxMaQ1VFUlBPIE5BQ0lPTkFM"+ 
            "IERFIFBPTElDSUExGDAWBgNVBGETD1ZBVEVTLVMyODE2MDE1SDEXMBUGA1UEAxMOQVBQRE5JTU9WSUxQ"+             "UkUwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARBfpojvXY9rbDS0VB2THZuTjX7Ii807tkKnAZZwbIO"+             "Et3FdGykeOHv9tt5PxPD/kr2io50wL1r2MGawBTo7wBpo4IEYzCCBF8wDAYDVR0TAQH/BAIwADAOBgNV"+             "HQ8BAf8EBAMCBeAwHQYDVR0OBBYEFFIE+56N46OfWO0z/Yw9z3GQmIcmMB8GA1UdIwQYMBaAFA2n5MC0"+ 
            "15fkdNyGfFL+9N4yYjK8MIG6BggrBgEFBQcBAwSBrTCBqjAIBgYEAI5GAQEwCwYGBACORgEDAgEPMAgG"+ 
            "BgQAjkYBBDATBgYEAI5GAQYwCQYHBACORgEGAjByBgYEAI5GAQUwaDAyFixodHRwczovL3BraS5wb2xp"+             "Y2lhLmVzL2NucC9wdWJsaWNhY2lvbmVzL3BkcxMCZW4wMhYsaHR0cHM6Ly9wa2kucG9saWNpYS5lcy9j"+ 
            "bnAvcHVibGljYWNpb25lcy9wZHMTAmVzMGkGCCsGAQUFBwEBBF0wWzAiBggrBgEFBQcwAYYWaHR0cDov"+             "L29jc3AucG9saWNpYS5lczA1BggrBgEFBQcwAoYpaHR0cDovL3BraS5wb2xpY2lhLmVzL2NucC9jZXJ0"+             "cy9BQzAwNC5jcnQwggEuBgNVHSAEggElMIIBITCCAQYGCGCFVAECAWY5MIH5MDcGCCsGAQUFBwIBFito"+             "dHRwOi8vcGtpLnBvbGljaWEuZXMvY25wL3B1YmxpY2FjaW9uZXMvZHBjMIG9BggrBgEFBQcCAjCBsAyB"+ 
            "rVFDQzogc2VsbG8gZWxlY3Ryw7NuaWNvIGRlIEFkbWluaXN0cmFjacOzbiwgw7NyZ2FubyBvIGVudGlk"+             "YWQgZGUgZGVyZWNobyBww7pibGljbywgbml2ZWwgYWx0by4gQ29uc3VsdGUgbGFzIGNvbmRpY2lvbmVz"+             "IGRlIHVzbyBlbiBodHRwOi8vcGtpLnBvbGljaWEuZXMvY25wL3B1YmxpY2FjaW9uZXMvZHBjMAkGBwQA"+ 
            "i+xAAQMwCgYIYIVUAQMFBgEwgbUGA1UdHwSBrTCBqjCBp6AqoCiGJmh0dHA6Ly9wa2kucG9saWNpYS5l"+             "cy9jbnAvY3Jscy9DUkwuY3JsonmkdzB1MQswCQYDVQQGEwJFUzEoMCYGA1UECgwfRElSRUNDSU9OIEdF"+             "TkVSQUwgREUgTEEgUE9MSUNJQTEMMAoGA1UECwwDQ05QMRgwFgYDVQRhDA9WQVRFUy1TMjgxNjAxNUgx"+             "FDASBgNVBAMMC0FSQyBER1AgMDAyMIHNBgNVHREEgcUwgcKBDnBraUBwb2xpY2lhLmVzpDIwMDEuMCwG"+ 
            "CWCFVAEDBQYBARYfU0VMTE8gRUxFQ1RST05JQ08gREUgTklWRUwgQUxUT6Q7MDkxNzA1BglghVQBAwUG"+ 
            "AQIWKEFNQklUTyBERUwgQ1VFUlBPIE5BQ0lPTkFMIERFIExBIFBPTElDSUGkHDAaMRgwFgYJYIVUAQMF"+ 
            "BgEDFglTMjgxNjAxNUikITAfMR0wGwYJYIVUAQMFBgEFFg5BUFBETklNT1ZJTFBSRTAdBgNVHSUEFjAU"+ 
            "BggrBgEFBQcDBAYIKwYBBQUHAwIwDQYJKoZIhvcNAQELBQADggIBADRybjPKB0n/vmbyRnnZ5FgYp1qt"+             "F/UaozwxcwgAGpcxIFxNC9iqohC6DrAC6pO9MUzdbzB3VnKam6/gYsNJmXAkPf/2SEuZJBTqP3HlrRet"+ 
            "PPJ+BsTRDueN4nA5MWj7GGpYIvjci15Iz1RONgOrZpG2wT6kTH07KM7dJ0e2q0+iU4JH3dj9eFcNd+cs"+ 
            "NjOrWFTS55gDU2Pxjul33r1d2Vi3ymBpQCzgxX7RczwgYcrmtiWFbwpqc/ZmIqrqt6jI2vV2cxRr4s4v"+ 
            "wKY3RQf2rRvhF/39o9YvYUyjxWaR9/DjhF+LdOBUSJhU0OyAjvOYTtYHThWjMWAKEUrUU4ilBgbFZTwS"+ 
            "aFXCSB7kMAImMt93tmhzAx0lBfYP4NRK/H8L4cr1mnvqNI2NFGWiYFlIcySKcyqGqNjn7zlgQdRotnW1"+ 
            "rqvhe0UyQuO98uVkSBN3Xzo6VGTgVBEVquwP1QT9lgv5+7LtaycjKpADmX3m4tdf/whnHCtKVUpWA+iq"+ 
            "Pwjytqef2VjzoQIWX2knt2uHMHRBmt6ktR5vKelv0ewEZloYCsT+2SPuo3rpd9EOJkLl02O9UG7T0lhw"+             "1UvFkJ5wMfjK8+gc/5x5hGe8Fzcg4culTrIBTTq2HhQ45wBHRYUXNNOHGyi0AqC0Vo5JnB6NjDXkMkQr"+ 
            "WGmckU12Ztc72pg0"; 
 
        X509Certificate signerX509cert = 
                (X509Certificate) CertificateFactory.getInstance("X.509"). 
                generateCertificate( 
                        new ByteArrayInputStream(Base64.getDecoder().decode(signerPemCert))                 ); 
 
        PublicKey signerPubkey = signerX509cert.getPublicKey();
```


```java
// 
        // Datos firmados 
        // 
        byte[] toBeSigned = HexFormat.of().parseHex( 
            "dc037581759ea9b5267c34114bf91b662d5d785a71f94bb472ec71f971c13fa8f83fa8f807094009"+             "393939393939393952420a30312d30312d3139383044064341524d454e461345535041c3914f4c41"+ 
            "2045535041c3914f4c4148014d4c0a31372d30342d323033345082037c0000000c6a5020200d0a87"+             "0a00000014667479706a703220000000006a7032200000002d6a7032680000001669686472000001"+             "ec000001900001070700000000000f636f6c72010000000000110000032f6a703263ff4fff510029"+             "000000000190000001ec000000000000000000000190000001ec00000000000000000001070101ff"+ 
            "52000c00000001000504040000ff5c002342772076f076f076c06f006f006ee06750675067685005"+ 
            "5005504757d357d35762ff640025000143726561746564206279204f70656e4a5045472076657273"+             "696f6e20322e352e30ff90000a00000000029e0001ff93cfae8e14001e5744c90b9fbe9967e5de2b"+ 
            "9341c32d0ae417bb06f61c49c5c20f9403a8ece87101552ffa6274f1a1ed6cfe3382bf80157c3de7"+             "8d3f52e23dfd1f1919a162d40e11b804be3c5af52bcbc76ffd114a7007fe79ddc169c02d073609ef"+             "80108f58d8b225567c8e431e3b9bb7fef7c9bea102fbba999584e469770fcc69a90f0dd3481b9cee"+             "70ef118e0c51ccc1c3e4a243e35b07c342571a601aaf804901fef7f5f3ae4c7dd6157aed21351148"+ 
            "755e9295e925b1b0e2f8d4588683667fe58e4ba9852f085db359b10c44a85971f5d1316b44d9e4f7"+             "6c642ca9242d234bbeda731d43b6047cd79f81c4b7f89783f6598c2595433268ccb85bcc21cb61a0"+             "acff83801d84efff3aea24c6d04137ab133e4230a72704db22ac0e436d0ba880bb4787fb2d7d9506"+ 
            "93dd88c3e2d410f874283ad0a3fcced48e65952da38815615678cd85a20d1937b5732f0c034f2cfc"+             "617fc68f1941de699580de950676879895c02854b8e508fcd6578b1f81d5aaa9ed8ef4d84a4a2bee"+             "51e786e9b09cc9b5f1a85974e31e8f824993f58680263a918dd3870af4db2c5ce88f22442fcc989f"+             "adc02ff65e48151bfed898ab1b2e00e759aa1844db9ce98d97904deca6ea7319fb3d3839926cadba"+ 
            "789fb46d2d937077778485e68ea1b71a9e191675d60cd0dbd57d5a79e79edea17fb08bdecf01c982"+ 
            "6d6131b3b0d0c7d1f82f44d39c4ba502495d801ce79707d1bfb682dc644b1d4ff898d0e990de8bc6"+ 
            "4f6d5b36276ea8889f4c4ef61e2779af965d331f2c248fbac3ba1438a0ebc176bad055fa2159eea1"+ 
            "e06fa575ef1135f69b6e12d788fe2079cf388262281f70ce90362a3f7942a00e1a437f61072e1a6d"+             "7f3fa5f51a454e9cc8ebecf0928741cb609956cd8c5059079db1ca00e6a6a70dfbc907805f4080ff"+ 
            "d9801331372d30342d323032342031313a32383a3230");  
        // 
        // Firma         // 
        byte[] signature = HexFormat.of().parseHex( 
            "9881E4DA3427F7F8F0C4BB2E04514E46F97398AEF2734235BAAC261627609FC0" +             "EC0F3B591D592AAE5B40D85749C8768EF9B183AA1CBF7561C1C0E22B423EB660"); 
  
        if( signerPubkey instanceof java.security.interfaces.ECPublicKey ) {             // 
            // Verificación de firma 
            // 
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");             ecdsaVerify.initVerify(signerPubkey);
ecdsaVerify.update(toBeSigned); 
 
            ASN1EncodableVector v = new ASN1EncodableVector(); 
            BigInteger r = new BigInteger(1,  
                    Arrays.copyOfRange(signature, 0, signature.length / 2)                 ); 
            BigInteger s = new BigInteger(1,  
                    Arrays.copyOfRange(signature, signature.length / 2, signature.length) 
                ); 
            v.add(new ASN1Integer(r)); 
            v.add(new ASN1Integer(s)); 
            DERSequence seq = new DERSequence(v); 
 
            boolean verified = ecdsaVerify.verify(seq.getEncoded());             if( verified ) { 
                System.out.println("Firma correcta"); 
            }             else { 
                System.out.println("Firma incorrecta"); 
            }         }         else { 
            System.out.println("Clave de firma incorrecta"); 
        } 
 
 
    } catch (CertificateException | IOException | SignatureException |              NoSuchAlgorithmException | InvalidKeyException e) {         throw new RuntimeException(e); 
    } 
}
```
```
