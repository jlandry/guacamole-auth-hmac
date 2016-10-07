<?php
include mcrypt
var message= "This is a sample message";

function encrypt($message, $initialVector, $secretKey) {
    return base64_encode(
        mcrypt_encrypt(
            MCRYPT_RIJNDAEL_128,
            md5($secretKey),
            $message,
            MCRYPT_MODE_CFB,
            $initialVector
        )
    );
}

?>
