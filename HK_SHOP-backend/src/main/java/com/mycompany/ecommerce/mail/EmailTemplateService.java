package com.mycompany.ecommerce.mail;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildActivationCodeEmail(String username, String code, long minutes) {
        return """
                Bonjour %s,

                Votre code d’activation HK Shop est : %s

                Il expire dans %d minutes.

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                """.formatted(username, code, minutes);
    }
}