package tofi.api.adm.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordGenerator {

    // Наборы символов для пароля
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS    = "0123456789";
    private static final String SPECIAL   = "!@#$%^&*()-_=+[]{}";

    private static final SecureRandom random = new SecureRandom();

    public static String generateTemporaryPassword(int length) {
        if (length < 8) length = 8; // Минимальная безопасная длина

        List<Character> passwordChars = new ArrayList<>();

        // Гарантируем наличие хотя бы одного символа из каждой группы
        passwordChars.add(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        passwordChars.add(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        passwordChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        passwordChars.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        // Объединяем все символы для заполнения оставшейся длины
        String allCharacters = LOWERCASE + UPPERCASE + DIGITS + SPECIAL;

        for (int i = 4; i < length; i++) {
            passwordChars.add(allCharacters.charAt(random.nextInt(allCharacters.length())));
        }

        // Перемешиваем символы, чтобы пароль не начинался всегда в одном формате
        Collections.shuffle(passwordChars, random);

        // Собираем в строку
        StringBuilder password = new StringBuilder();
        for (char ch : passwordChars) {
            password.append(ch);
        }
        return password.toString();
    }

    public static boolean checkPasswd(String passwd) {
        // Проверка на null, чтобы избежать NullPointerException
        if (passwd == null) {
            return false;
        }

        String regex = "[!@#^&_]";
        boolean res = passwd.length() > 7;

        // matches в Java проверяет, соответствует ли вся строка шаблону
        res = res && passwd.matches(".*[A-Z].*");
        res = res && passwd.matches(".*[a-z].*");
        res = res && passwd.matches(".*\\d.*");

        // Для аналога .find() используем Pattern и Matcher
        Pattern pattern = Pattern.compile(regex);
        res = res && pattern.matcher(passwd).find();

        return res;
    }


}
