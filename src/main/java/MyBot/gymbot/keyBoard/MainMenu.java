package MyBot.gymbot.keyBoard;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static MyBot.gymbot.service.BotCommand.*;

public class MainMenu {
    public ReplyKeyboard createReplyKeyboard (Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText("Выбери тренировку");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true); // Автоматическое изменение размера
        keyboard.setOneTimeKeyboard(false); // Скрывать клавиатуру после нажатия
        keyboard.setSelective(false);
//
        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Первая строка
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(BACK_MUSCLES.getCommand()));
        row1.add(new KeyboardButton(CHEST.getCommand()));
        row1.add(new KeyboardButton(LEGS.getCommand()));
        // Вторая строка
//        KeyboardRow row2 = new KeyboardRow();

        // Третья строка
//        KeyboardRow row3 = new KeyboardRow();


        keyboardRows.add(row1);
//        keyboardRows.add(row2);
//        keyboardRows.add(row3);

        keyboard.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboard);

        return keyboard;
    }
}
